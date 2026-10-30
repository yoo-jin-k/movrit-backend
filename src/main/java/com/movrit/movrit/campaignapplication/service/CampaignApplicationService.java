package com.movrit.movrit.campaignapplication.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.dto.CampaignParticipantDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.service.CampaignContentService;
import com.movrit.movrit.campaigneligibility.service.CampaignEligibilityPolicyService;
import com.movrit.movrit.campaigneligibility.service.MemberChannelMetricService;
import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignSelectionMode;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.service.CreatorService;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 캠페인 신청의 전 과정을 처리합니다.
 *
 * <p>업체의 신청서 양식 저장, 참여자의 신청 가능 여부 확인과 신청, 선정·미선정, 참여 수락, 체험 완료 확인, 취소, 그리고 참여자·업체·운영자 화면용 목록 조회를
 * 맡습니다. 선정은 캠페인의 선정 방식에 따라 소유 업체(BRAND_DIRECT) 또는 운영자(OPERATOR)만 할 수 있습니다. 상태가 바뀔 때마다 상대방에게 알림을 보내고
 * 감사 로그를 남깁니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignApplicationService {
  private final CampaignRepository campaignRepository;
  private final CampaignApplicationFormRepository formRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final CreatorService creatorService;
  private final MemberService memberService;
  private final CampaignEligibilityPolicyService eligibilityPolicyService;
  private final MemberChannelMetricService memberChannelMetricService;
  private final SellerProfileService sellerProfileService;
  private final CampaignOwnershipService campaignOwnership;
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;
  private final com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService visitEvidence;
  private final CampaignContentService campaignContentService;

  /**
   * 캠페인 소유 업체가 신청서 양식(체험 방식, 질문, 동의 문서)을 만들거나 고칩니다. 버전 확인 없이 저장하는 기존 경로입니다.
   *
   * @throws ResponseStatusException 소유 캠페인이 아니거나 질문·동의 문서 ID가 중복되면 거절합니다
   */
  @Transactional
  public CampaignApplicationDtos.FormResponse saveForm(
      Long sellerMemberId, Long campaignId, CampaignApplicationDtos.FormSaveRequest request) {
    return saveForm(sellerMemberId, campaignId, request, null, false);
  }

  /**
   * 신청서 양식을 저장하되, 화면이 본 버전과 현재 버전이 같을 때만 저장합니다. 다른 사람이 먼저 고친 내용을 덮어쓰지 않기 위해 사용합니다.
   *
   * @throws ResponseStatusException 버전이 다르면 409
   */
  @Transactional
  public CampaignApplicationDtos.FormResponse saveFormWithExpectedVersion(
      Long sellerMemberId,
      Long campaignId,
      CampaignApplicationDtos.FormSaveRequest request,
      Long expectedVersion) {
    return saveForm(sellerMemberId, campaignId, request, expectedVersion, true);
  }

  /** 양식이 없으면 버전 1로 새로 만들고, 있으면 내용을 교체하면서 버전을 올립니다. */
  private CampaignApplicationDtos.FormResponse saveForm(
      Long sellerMemberId,
      Long campaignId,
      CampaignApplicationDtos.FormSaveRequest request,
      Long expectedVersion,
      boolean requireExpectedVersion) {
    Campaign campaign = getOwnedCampaign(sellerMemberId, campaignId);
    validateDefinition(request);

    CampaignApplicationForm form =
        formRepository
            .findByCampaignId(campaignId)
            .orElseGet(
                () ->
                    CampaignApplicationForm.builder()
                        .campaign(campaign)
                        .allowedFulfillmentTypesJson(write(request.allowedFulfillmentTypes()))
                        .questionsJson(write(request.questions()))
                        .agreementsJson(write(request.agreements()))
                        .version(1L)
                        .build());
    if (form.getId() != null) {
      if (requireExpectedVersion && !form.getVersion().equals(expectedVersion)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 신청서가 변경되었습니다. 다시 조회해 주세요.");
      }
      form.replace(
          write(request.allowedFulfillmentTypes()),
          write(request.questions()),
          write(request.agreements()));
    }
    return formResponse(formRepository.save(form));
  }

  /**
   * 캠페인의 공개 신청서 양식을 조회합니다. 로그인하지 않아도 볼 수 있습니다.
   *
   * @throws ResponseStatusException 캠페인이나 양식이 없으면 404
   */
  public CampaignApplicationDtos.FormResponse getForm(Long campaignId) {
    getCampaign(campaignId);
    return formResponse(getFormEntity(campaignId));
  }

  /**
   * 로그인한 회원이 이 캠페인에 지금 신청할 수 있는지 판정합니다.
   *
   * <p>모집 기간, 이메일 소유 확인, 캠페인별 채널 지표 조건, 중복 신청 여부를 확인해 막는 사유 목록(reasons)을 돌려줍니다. 사유가 없으면 신청할 수 있습니다.
   * 화면 안내용이며 실제 신청 시 서버가 같은 조건을 다시 검사합니다.
   */
  public CampaignApplicationDtos.EligibilityResponse getEligibility(
      Long memberId, Long campaignId) {
    Campaign campaign = getCampaign(campaignId);
    CampaignApplicationForm form = getFormEntity(campaignId);
    Member member = memberService.getMember(memberId);
    Creator creator = creatorService.findOptionalByMember(memberId).orElse(null);
    CampaignEligibilityPolicyService.Evaluation evaluation =
        eligibilityPolicyService.evaluate(memberId, campaignId);
    List<String> reasons = campaignEligibilityReasons(member, campaign, evaluation);
    boolean alreadyApplied =
        applicationRepository.existsByCampaignIdAndCreatorMemberId(campaignId, memberId);
    if (alreadyApplied) {
      reasons.add("ALREADY_APPLIED");
    }
    return new CampaignApplicationDtos.EligibilityResponse(
        campaignId,
        form.getVersion(),
        "EMAIL_AND_CAMPAIGN_REQUIREMENT",
        member.isEmailVerified(),
        false,
        creator != null && hasChannel(creator),
        memberChannelMetricService.findMine(memberId).profileExists(),
        evaluation.requirement(),
        evaluation.metricEvaluation(),
        alreadyApplied,
        reasons.isEmpty(),
        List.copyOf(reasons));
  }

  /**
   * 참여자가 캠페인에 신청합니다.
   *
   * <p>검사 순서: 모집 중인지 → 화면이 본 양식 버전이 최신인지 → 이메일 확인과 채널 지표 조건 → 조건 버전 일치 → 중복 신청 → 체험 방식·배송지·질문 답변·필수
   * 동의. 모두 통과하면 배송지와 답변, 동의, 당시 조건을 신청에 복사해 저장하고 업체에 알림을 보냅니다. 캠페인 행을 잠가 같은 캠페인의 동시 신청과 방문 정보 기록
   * 순서를 지킵니다.
   *
   * @throws ResponseStatusException 입력 오류는 400, 모집 기간·조건·버전·중복 신청 문제는 409
   */
  @Transactional
  public CampaignApplicationDtos.Response apply(
      Long memberId, Long campaignId, CampaignApplicationDtos.CreateRequest request) {
    // 주소 변경과 신청 증빙 저장은 같은 캠페인 잠금으로 순서를 보장합니다.
    campaignRepository
        .findByIdForUpdate(campaignId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    Campaign campaign = getOpenCampaign(campaignId);
    CampaignApplicationForm form = getFormEntity(campaignId);
    Member member = memberService.getMember(memberId);

    if (!form.getVersion().equals(request.expectedFormVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "신청 조건이 변경되었습니다. 최신 신청서를 다시 확인해 주세요.");
    }
    CampaignEligibilityPolicyService.Evaluation eligibility = validateEligibility(member, campaign);
    validateExpectedRequirement(request, eligibility.requirement());
    if (applicationRepository.existsByCampaignIdAndCreatorMemberId(campaignId, memberId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
    }
    validateApplication(form, request);
    Creator creator = creatorService.getOrCreateCandidate(memberId);

    CampaignApplication application =
        CampaignApplication.builder()
            .campaign(campaign)
            .creator(creator)
            .fulfillmentType(request.fulfillmentType())
            .status(CampaignApplicationStatus.SUBMITTED)
            .message(request.message().trim())
            .recipientName(
                contactValue(request, CampaignApplicationDtos.ShippingContact::recipientName))
            .recipientPhone(
                contactValue(request, CampaignApplicationDtos.ShippingContact::recipientPhone))
            .postalCode(contactValue(request, CampaignApplicationDtos.ShippingContact::postalCode))
            .address1(contactValue(request, CampaignApplicationDtos.ShippingContact::address1))
            .address2(contactValue(request, CampaignApplicationDtos.ShippingContact::address2))
            .answersJson(write(request.answers()))
            .agreementAcceptancesJson(write(request.agreementAcceptances()))
            .formVersion(form.getVersion())
            .eligibilityRequirementSnapshotJson(
                eligibility.requirement() == null ? null : write(eligibility.requirement()))
            .eligibilityRequirementVersion(
                eligibility.requirement() == null ? null : eligibility.requirement().version())
            // 본인 인증 공급자가 정해지기 전에는 인증 완료로 간주하지 않습니다.
            .identityVerificationConfigured(false)
            .identityVerified(false)
            .emailVerified(member.isEmailVerified())
            .channelReady(hasChannel(creator))
            .submittedAt(LocalDateTime.now())
            .build();
    try {
      CampaignApplication saved = applicationRepository.saveAndFlush(application);
      visitEvidence.capture(saved);
      Long sellerMemberId = campaign.getBrand().getSeller().getMember().getId();
      notificationService.send(
          sellerMemberId,
          "CAMPAIGN_APPLICATION",
          "새 캠페인 신청이 접수되었습니다.",
          creator.getNickname() + "님이 캠페인에 신청하셨습니다.",
          "CAMPAIGN_APPLICATION",
          saved.getId());
      auditLogService.record(
          memberId,
          "CAMPAIGN_APPLICATION_SUBMITTED",
          "CAMPAIGN_APPLICATION",
          saved.getId(),
          null,
          "{\"status\":\"SUBMITTED\"}",
          null);
      return response(saved);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
    }
  }

  /** 로그인한 참여자의 신청 목록을 최신순으로 조회합니다. */
  public List<CampaignApplicationDtos.Response> findMine(Long memberId) {
    return applicationRepository.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(this::response)
        .toList();
  }

  /**
   * '내 캠페인' 화면용으로 신청별 진행 정보(신청, 캠페인 일정·안내, 담당자 연락처, 콘텐츠 제출 상태)를 묶어 조회합니다.
   *
   * <p>브랜드 담당자 연락처는 선정 이후 단계에서만 보여주고, 접수·미선정·취소 상태에서는 숨깁니다.
   */
  public List<CampaignApplicationDtos.WorkspaceResponse> findMyWorkspaces(Long memberId) {
    return applicationRepository.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(
            application -> {
              Campaign campaign = application.getCampaign();
              boolean contactVisible =
                  application.getStatus() != CampaignApplicationStatus.SUBMITTED
                      && application.getStatus() != CampaignApplicationStatus.REJECTED
                      && application.getStatus() != CampaignApplicationStatus.CANCELLED;
              CampaignManagementDtos.ContactResponse contact =
                  !contactVisible
                          || campaign.getContactName() == null
                          || campaign.getContactPhone() == null
                      ? null
                      : new CampaignManagementDtos.ContactResponse(
                          campaign.getContactName(), campaign.getContactPhone());
              return new CampaignApplicationDtos.WorkspaceResponse(
                  response(application),
                  CampaignManagementDtos.DetailResponse.from(
                      campaign, application.getFulfillmentType() == FulfillmentType.VISIT),
                  contact,
                  campaignContentService.findMineByApplication(memberId, application.getId()));
            })
        .toList();
  }

  /**
   * 승인된 업체가 자기 캠페인에 들어온 신청을 최신순으로 조회합니다(참여자 관리 화면).
   *
   * <p>참여자별 완료 캠페인 수와 채널 지표를 한 번에 모아 붙여, 신청마다 따로 조회하지 않게 합니다. 배송 연락처는 참여 수락 이후에만 보입니다.
   */
  public List<CampaignApplicationDtos.Response> findSellerApplications(Long memberId) {
    sellerProfileService.getApprovedSeller(memberId);
    List<CampaignApplication> applications =
        applicationRepository.findAllByCampaignBrandSellerMemberIdOrderByIdDesc(memberId).stream()
            .toList();
    Set<Long> creatorIds =
        applications.stream()
            .map(application -> application.getCreator().getId())
            .collect(java.util.stream.Collectors.toSet());
    Map<Long, Long> completedCounts = new HashMap<>();
    if (!creatorIds.isEmpty()) {
      applicationRepository
          .countCompletedCampaignsByCreatorIds(creatorIds)
          .forEach(count -> completedCounts.put(count.getCreatorId(), count.getCompletedCount()));
    }
    Map<Long, List<CampaignParticipantDtos.ChannelMetric>> metricsByMember = new HashMap<>();
    Set<Long> memberIds =
        applications.stream()
            .map(application -> application.getCreator().getMember().getId())
            .collect(java.util.stream.Collectors.toSet());
    if (!memberIds.isEmpty()) {
      applicationRepository
          .findMetricSummariesByMemberIds(memberIds)
          .forEach(
              metric ->
                  metricsByMember
                      .computeIfAbsent(metric.getMemberId(), key -> new ArrayList<>())
                      .add(metric(metric)));
    }
    return applications.stream()
        .map(
            application ->
                sellerResponse(
                    application,
                    completedCounts.getOrDefault(application.getCreator().getId(), 0L),
                    metricsByMember.getOrDefault(
                        application.getCreator().getMember().getId(), List.of())))
        .toList();
  }

  /**
   * 운영자가 선정하는 캠페인(OPERATOR)의 신청만 운영자에게 보여줍니다.
   *
   * @throws ResponseStatusException 운영자가 아니면 거절합니다
   */
  public List<CampaignApplicationDtos.Response> findOperatorApplications(Long adminMemberId) {
    memberService.getAdmin(adminMemberId);
    return applicationRepository
        .findAllByCampaignSelectionModeOrderByIdDesc(CampaignSelectionMode.OPERATOR)
        .stream()
        .map(this::sellerResponse)
        .toList();
  }

  /**
   * 캠페인 소유 업체가 신청을 선정합니다. 업체 직접 선정(BRAND_DIRECT) 캠페인에서만 가능합니다.
   *
   * @throws ResponseStatusException 소유 업체가 아니거나 선정 주체가 다르면 거절, 모집 인원이 찼거나 접수 상태가 아니면 409
   */
  @Transactional
  public CampaignApplicationDtos.Response select(Long memberId, Long applicationId) {
    CampaignApplication application = getSellerApplication(memberId, applicationId);
    requireSelectionMode(application, CampaignSelectionMode.BRAND_DIRECT);
    return selectApplication(memberId, application);
  }

  /**
   * 운영자가 신청을 선정합니다. 운영자 선정(OPERATOR) 캠페인에서만 가능합니다.
   *
   * @throws ResponseStatusException 운영자가 아니거나 선정 주체가 다르면 거절, 모집 인원이 찼거나 접수 상태가 아니면 409
   */
  @Transactional
  public CampaignApplicationDtos.Response operatorSelect(Long adminMemberId, Long applicationId) {
    memberService.getAdmin(adminMemberId);
    CampaignApplication application = getApplication(applicationId);
    requireSelectionMode(application, CampaignSelectionMode.OPERATOR);
    return selectApplication(adminMemberId, application);
  }

  /**
   * 선정 공통 처리입니다.
   *
   * <p>캠페인 행을 잠근 뒤 이미 선정·진행·완료된 인원을 세어 모집 인원을 넘지 않는지 확인합니다. 잠금 덕분에 두 사람이 동시에 선정해도 인원을 초과하지 않습니다.
   */
  private CampaignApplicationDtos.Response selectApplication(
      Long actorMemberId, CampaignApplication application) {
    Campaign lockedCampaign =
        campaignRepository
            .findByIdForUpdate(application.getCampaign().getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    long selectedCount =
        applicationRepository.countByCampaignIdAndStatusIn(
            lockedCampaign.getId(),
            List.of(
                CampaignApplicationStatus.SELECTED,
                CampaignApplicationStatus.ACTIVE,
                CampaignApplicationStatus.CONTENT_SUBMITTED,
                CampaignApplicationStatus.REVISION_REQUESTED,
                CampaignApplicationStatus.COMPLETED));
    if (selectedCount >= lockedCampaign.getRecruitmentCount()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 모집 인원이 모두 선정되었습니다.");
    }
    CampaignApplicationDtos.Response response =
        transition(application, CampaignApplication::select);
    notifyCreatorAndAudit(
        actorMemberId,
        application,
        "신청이 선정되었습니다.",
        "CAMPAIGN_APPLICATION_SELECTED",
        "SELECTED",
        null);
    return response;
  }

  /**
   * 캠페인 소유 업체가 신청을 미선정 처리합니다. 업체 직접 선정 캠페인에서만 가능합니다.
   *
   * @throws ResponseStatusException 소유 업체가 아니거나 선정 주체가 다르면 거절, 접수 상태가 아니면 409
   */
  @Transactional
  public CampaignApplicationDtos.Response reject(Long memberId, Long applicationId) {
    CampaignApplication application = getSellerApplication(memberId, applicationId);
    requireSelectionMode(application, CampaignSelectionMode.BRAND_DIRECT);
    return rejectApplication(memberId, application);
  }

  /**
   * 운영자가 신청을 미선정 처리합니다. 운영자 선정 캠페인에서만 가능합니다.
   *
   * @throws ResponseStatusException 운영자가 아니거나 선정 주체가 다르면 거절, 접수 상태가 아니면 409
   */
  @Transactional
  public CampaignApplicationDtos.Response operatorReject(Long adminMemberId, Long applicationId) {
    memberService.getAdmin(adminMemberId);
    CampaignApplication application = getApplication(applicationId);
    requireSelectionMode(application, CampaignSelectionMode.OPERATOR);
    return rejectApplication(adminMemberId, application);
  }

  /** 미선정 공통 처리입니다. 상태를 바꾸고 참여자에게 결과를 알립니다. */
  private CampaignApplicationDtos.Response rejectApplication(
      Long actorMemberId, CampaignApplication application) {
    CampaignApplicationDtos.Response response =
        transition(application, CampaignApplication::reject);
    notifyCreatorAndAudit(
        actorMemberId,
        application,
        "신청이 선정되지 않았습니다.",
        "CAMPAIGN_APPLICATION_REJECTED",
        "REJECTED",
        null);
    return response;
  }

  /**
   * 선정된 참여자가 본인 신청의 참여를 수락합니다. 선정에서 진행 중으로 바뀌고 업체에 알림을 보냅니다.
   *
   * <p>이때부터 업체가 배송 연락처를 볼 수 있습니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 선정 상태가 아니면 409
   */
  @Transactional
  public CampaignApplicationDtos.Response activate(Long memberId, Long applicationId) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    CampaignApplicationDtos.Response response =
        transition(application, CampaignApplication::activate);
    Long sellerMemberId = application.getCampaign().getBrand().getSeller().getMember().getId();
    notificationService.send(
        sellerMemberId,
        "CAMPAIGN_APPLICATION",
        "캠페인 참여가 확정되었습니다.",
        application.getCreator().getNickname() + "님이 참여를 확정하셨습니다.",
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        memberId,
        "CAMPAIGN_APPLICATION_ACTIVATED",
        "CAMPAIGN_APPLICATION",
        application.getId(),
        "{\"status\":\"SELECTED\"}",
        "{\"status\":\"ACTIVE\"}",
        null);
    return response;
  }

  /**
   * 참여자가 본인 신청을 취소합니다. 접수·선정·진행 중 상태에서만 가능하고, 취소 사유를 기록해 업체에 알립니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 콘텐츠 제출 이후 등 취소할 수 없는 상태면 409
   */
  @Transactional
  public CampaignApplicationDtos.Response cancel(
      Long memberId, Long applicationId, CampaignApplicationDtos.CancelRequest request) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    CampaignApplicationStatus previousStatus = application.getStatus();
    CampaignApplicationDtos.Response response =
        transition(application, value -> value.cancel(request.reason().trim()));
    Long sellerMemberId = application.getCampaign().getBrand().getSeller().getMember().getId();
    notificationService.send(
        sellerMemberId,
        "CAMPAIGN_APPLICATION",
        "캠페인 참여가 취소되었습니다.",
        application.getCreator().getNickname() + "님이 참여를 취소하셨습니다.",
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        memberId,
        "CAMPAIGN_APPLICATION_CANCELLED",
        "CAMPAIGN_APPLICATION",
        application.getId(),
        "{\"status\":\"" + previousStatus + "\"}",
        "{\"status\":\"CANCELLED\"}",
        request.reason().trim());
    return response;
  }

  /**
   * 참여자가 체험 완료(배송 수령 또는 방문)를 확인합니다. 확인 후에 콘텐츠를 제출할 수 있으며 업체에 알림을 보냅니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 진행 중 상태가 아니면 409
   */
  @Transactional
  public CampaignApplicationDtos.Response confirmExperience(Long memberId, Long applicationId) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    CampaignApplicationDtos.Response response =
        transition(application, CampaignApplication::confirmExperience);
    Long sellerMemberId = application.getCampaign().getBrand().getSeller().getMember().getId();
    notificationService.send(
        sellerMemberId,
        "CAMPAIGN_EXPERIENCE",
        "체험 완료가 확인되었습니다.",
        application.getCreator().getNickname() + "님이 체험 완료를 확인하셨습니다.",
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        memberId,
        "CAMPAIGN_EXPERIENCE_CONFIRMED",
        "CAMPAIGN_APPLICATION",
        application.getId(),
        null,
        "{\"experienceConfirmed\":true}",
        null);
    return response;
  }

  /** 신청서 양식 자체를 검사합니다. 질문·동의 문서 ID 중복과 선택지 없는 단일 선택 질문을 거절합니다. */
  private void validateDefinition(CampaignApplicationDtos.FormSaveRequest request) {
    requireUniqueIds(
        request.questions().stream().map(CampaignApplicationDtos.QuestionDefinition::id).toList(),
        "질문 ID");
    requireUniqueIds(
        request.agreements().stream().map(CampaignApplicationDtos.AgreementDefinition::id).toList(),
        "동의 문서 ID");
    boolean invalidChoice =
        request.questions().stream()
            .anyMatch(
                question ->
                    question.type() == CampaignApplicationDtos.QuestionType.SINGLE_CHOICE
                        && question.options().isEmpty());
    if (invalidChoice) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단일 선택 질문에는 선택지가 필요합니다.");
    }
  }

  /** 신청 직전 자격을 검사합니다. 이메일 확인이 먼저이고, 그다음 캠페인별 채널 지표 조건을 판정해 막는 사유를 사용자 안내 문구로 바꿉니다. */
  private CampaignEligibilityPolicyService.Evaluation validateEligibility(
      Member member, Campaign campaign) {
    if (!member.isEmailVerified()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이메일 소유 확인 후 캠페인에 신청할 수 있습니다.");
    }
    CampaignEligibilityPolicyService.Evaluation evaluation =
        eligibilityPolicyService.evaluate(member.getId(), campaign.getId());
    String reason = evaluation.blockingReason();
    if (reason == null) return evaluation;
    String message =
        switch (reason) {
          case "CHANNEL_METRIC_REQUIRED" -> "캠페인 신청에 필요한 채널 지표를 먼저 등록해 주세요.";
          case "CHANNEL_METRIC_UNVERIFIED" -> "캠페인 신청에 필요한 채널 지표가 아직 확인되지 않았습니다.";
          case "CHANNEL_METRIC_BELOW_MINIMUM" -> "캠페인의 채널 지표 조건을 충족하지 못했습니다.";
          default -> "캠페인 신청 조건을 확인해 주세요.";
        };
    throw new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  /** 화면이 보고 있던 참여 조건(ID·버전)이 현재 조건과 같은지 확인합니다. 신청 도중 업체가 조건을 바꾸면 다시 확인하도록 거절합니다. */
  private void validateExpectedRequirement(
      CampaignApplicationDtos.CreateRequest request,
      com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos.RequirementResponse
          current) {
    Long currentId = current == null ? null : current.id();
    Long currentVersion = current == null ? null : current.version();
    if (!java.util.Objects.equals(request.expectedEligibilityRequirementId(), currentId)
        || !java.util.Objects.equals(
            request.expectedEligibilityRequirementVersion(), currentVersion)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "캠페인 신청 조건이 변경되었습니다. 최신 조건을 다시 확인해 주세요.");
    }
  }

  /** 신청을 막는 사유 코드를 모읍니다(모집 기간 아님, 이메일 미확인, 채널 지표 조건 미충족). */
  private List<String> campaignEligibilityReasons(
      Member member, Campaign campaign, CampaignEligibilityPolicyService.Evaluation evaluation) {
    List<String> reasons = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    if (!campaign.isOpen()
        || now.isBefore(campaign.getStartAt())
        || !now.isBefore(campaign.getEndAt())) {
      reasons.add("CAMPAIGN_NOT_OPEN");
    }
    if (!member.isEmailVerified()) {
      reasons.add("EMAIL_VERIFICATION_REQUIRED");
    }
    if (evaluation.blockingReason() != null) reasons.add(evaluation.blockingReason());
    return reasons;
  }

  /** 활동 프로필에 SNS 주소가 등록되어 있는지 확인합니다. */
  private boolean hasChannel(Creator creator) {
    return creator.getSnsUrl() != null && !creator.getSnsUrl().isBlank();
  }

  /** 신청서 내용을 검사합니다. 허용된 체험 방식인지, 배송 정보, 질문 답변(중복·누락·글자 수·선택지), 필수 동의 문서 순서로 확인합니다. */
  private void validateApplication(
      CampaignApplicationForm form, CampaignApplicationDtos.CreateRequest request) {
    List<FulfillmentType> allowedTypes =
        read(form.getAllowedFulfillmentTypesJson(), new TypeReference<>() {});
    if (!allowedTypes.contains(request.fulfillmentType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이 캠페인에서 지원하지 않는 참여 방식입니다.");
    }
    validateShippingContact(request);

    List<CampaignApplicationDtos.QuestionDefinition> questions =
        read(form.getQuestionsJson(), new TypeReference<>() {});
    Map<String, String> answers = new HashMap<>();
    for (CampaignApplicationDtos.Answer answer : request.answers()) {
      if (answers.put(answer.questionId(), answer.value()) != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 질문에 답변을 두 번 보낼 수 없습니다.");
      }
    }
    Set<String> knownQuestionIds =
        questions.stream()
            .map(CampaignApplicationDtos.QuestionDefinition::id)
            .collect(java.util.stream.Collectors.toSet());
    if (!knownQuestionIds.containsAll(answers.keySet())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신청서에 없는 질문 답변이 포함되어 있습니다.");
    }
    boolean missingRequiredAnswer =
        questions.stream()
            .filter(CampaignApplicationDtos.QuestionDefinition::required)
            .anyMatch(question -> isBlank(answers.get(question.id())));
    if (missingRequiredAnswer) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 질문에 모두 답변해 주세요.");
    }

    for (CampaignApplicationDtos.QuestionDefinition question : questions) {
      String value = answers.get(question.id());
      if (value == null) {
        continue;
      }
      if (question.maxLength() != null && value.length() > question.maxLength()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            question.label() + " 답변은 " + question.maxLength() + "자 이내로 입력해 주세요.");
      }
      if (question.type() == CampaignApplicationDtos.QuestionType.SINGLE_CHOICE
          && !question.options().contains(value)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문의 선택지에 없는 답변입니다.");
      }
    }

    validateAgreements(form, request.agreementAcceptances());
  }

  /** 배송형은 수령인·연락처·우편번호·주소를 모두 요구하고, 방문형은 배송 주소를 받지 않습니다. */
  private void validateShippingContact(CampaignApplicationDtos.CreateRequest request) {
    CampaignApplicationDtos.ShippingContact contact = request.shippingContact();
    if (request.fulfillmentType() == FulfillmentType.VISIT) {
      if (contact != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방문형 캠페인은 배송 주소를 수집하지 않습니다.");
      }
      return;
    }
    if (contact == null
        || isBlank(contact.recipientName())
        || isBlank(contact.recipientPhone())
        || isBlank(contact.postalCode())
        || isBlank(contact.address1())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송형 캠페인의 수령 정보를 모두 입력해 주세요.");
    }
  }

  /** 필수 동의 문서마다 최신 버전에 동의했는지 확인합니다. 같은 문서를 두 번 보내면 거절합니다. */
  private void validateAgreements(
      CampaignApplicationForm form, List<CampaignApplicationDtos.AgreementAcceptance> acceptances) {
    List<CampaignApplicationDtos.AgreementDefinition> agreements =
        read(form.getAgreementsJson(), new TypeReference<>() {});
    Map<String, CampaignApplicationDtos.AgreementAcceptance> acceptedById = new HashMap<>();
    for (CampaignApplicationDtos.AgreementAcceptance acceptance : acceptances) {
      if (acceptedById.put(acceptance.agreementId(), acceptance) != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 동의 문서를 두 번 제출할 수 없습니다.");
      }
    }
    for (CampaignApplicationDtos.AgreementDefinition agreement : agreements) {
      CampaignApplicationDtos.AgreementAcceptance acceptance = acceptedById.get(agreement.id());
      if (agreement.required()
          && (acceptance == null
              || !acceptance.accepted()
              || !agreement.version().equals(acceptance.version()))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 동의 문서의 최신 버전에 동의해 주세요.");
      }
    }
  }

  private void requireUniqueIds(List<String> ids, String label) {
    if (new HashSet<>(ids).size() != ids.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "가 중복되었습니다.");
    }
  }

  /** 공개 중이고 모집 기간 안에 있는 캠페인만 돌려줍니다. 마감 시각은 포함하지 않습니다. */
  private Campaign getOpenCampaign(Long id) {
    Campaign campaign = getCampaign(id);
    LocalDateTime now = LocalDateTime.now();
    if (!campaign.isOpen()
        || now.isBefore(campaign.getStartAt())
        || !now.isBefore(campaign.getEndAt())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 신청할 수 없는 캠페인입니다.");
    }
    return campaign;
  }

  /** 캠페인을 찾고, 요청한 업체 회원이 연결 상품의 소유자인지 확인합니다. */
  private Campaign getOwnedCampaign(Long memberId, Long id) {
    Campaign campaign = getCampaign(id);
    campaignOwnership.requireOwner(memberId, campaign);
    return campaign;
  }

  private Campaign getCampaign(Long id) {
    return campaignRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
  }

  private CampaignApplicationForm getFormEntity(Long campaignId) {
    return formRepository
        .findByCampaignId(campaignId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인 신청서가 아직 구성되지 않았습니다."));
  }

  /** 신청을 찾고, 요청한 업체 회원이 해당 캠페인의 소유자인지 확인합니다. */
  private CampaignApplication getSellerApplication(Long memberId, Long applicationId) {
    CampaignApplication application = getApplication(applicationId);
    getOwnedCampaign(memberId, application.getCampaign().getId());
    return application;
  }

  private CampaignApplication getApplication(Long applicationId) {
    return applicationRepository
        .findById(applicationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
  }

  /** 캠페인의 선정 주체가 기대한 방식(업체 직접 또는 운영자)인지 확인합니다. 다르면 403으로 거절합니다. */
  private void requireSelectionMode(
      CampaignApplication application, CampaignSelectionMode expected) {
    if (application.getCampaign().getSelectionMode() != expected) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 캠페인의 선정 주체가 아닙니다.");
    }
  }

  private CampaignApplicationDtos.FormResponse formResponse(CampaignApplicationForm form) {
    return new CampaignApplicationDtos.FormResponse(
        form.getCampaign().getId(),
        form.getVersion(),
        read(form.getAllowedFulfillmentTypesJson(), new TypeReference<>() {}),
        read(form.getQuestionsJson(), new TypeReference<>() {}),
        read(form.getAgreementsJson(), new TypeReference<>() {}));
  }

  private CampaignApplicationDtos.Response response(CampaignApplication application) {
    return response(application, false);
  }

  private CampaignApplicationDtos.Response sellerResponse(CampaignApplication application) {
    return sellerResponse(application, null);
  }

  private CampaignApplicationDtos.Response sellerResponse(
      CampaignApplication application, Long completedCampaignCount) {
    return sellerResponse(application, completedCampaignCount, List.of());
  }

  private CampaignApplicationDtos.Response sellerResponse(
      CampaignApplication application,
      Long completedCampaignCount,
      List<CampaignParticipantDtos.ChannelMetric> creatorMetrics) {
    // 선정만으로는 참여가 확정되지 않습니다. 당사자가 수락해 ACTIVE가 된 뒤에만
    // 배송에 필요한 연락처를 업체에 제공하고, 취소·미선정 건에서는 다시 숨깁니다.
    boolean hideContact =
        application.getStatus() != CampaignApplicationStatus.ACTIVE
            && application.getStatus() != CampaignApplicationStatus.CONTENT_SUBMITTED
            && application.getStatus() != CampaignApplicationStatus.REVISION_REQUESTED
            && application.getStatus() != CampaignApplicationStatus.COMPLETED;
    return response(application, hideContact, completedCampaignCount, creatorMetrics);
  }

  private CampaignApplicationDtos.Response response(
      CampaignApplication application, boolean hideContact) {
    return response(application, hideContact, null);
  }

  private CampaignApplicationDtos.Response response(
      CampaignApplication application, boolean hideContact, Long completedCampaignCount) {
    return response(application, hideContact, completedCampaignCount, List.of());
  }

  private CampaignApplicationDtos.Response response(
      CampaignApplication application,
      boolean hideContact,
      Long completedCampaignCount,
      List<CampaignParticipantDtos.ChannelMetric> creatorMetrics) {
    CampaignApplicationDtos.ShippingContact contact =
        application.getFulfillmentType() == FulfillmentType.DELIVERY && !hideContact
            ? new CampaignApplicationDtos.ShippingContact(
                application.getRecipientName(),
                application.getRecipientPhone(),
                application.getPostalCode(),
                application.getAddress1(),
                application.getAddress2())
            : null;
    List<String> notices = new ArrayList<>();
    if (!application.isIdentityVerificationConfigured()) notices.add("본인 인증 기능이 아직 구성되지 않았습니다.");
    if (!application.isChannelReady()) notices.add("활동 채널 정보를 먼저 등록해 주세요.");
    return new CampaignApplicationDtos.Response(
        application.getId(),
        application.getCampaign().getId(),
        application.getCampaign().getTitle(),
        application.getCampaign().getSelectionMode(),
        // 프리오더 없이 운영하는 캠페인은 연결 프리오더 ID를 비워 응답합니다.
        application.getCampaign().getPreorder() == null
            ? null
            : application.getCampaign().getPreorder().getId(),
        application.getCreator().getId(),
        application.getCreator().getNickname(),
        application.getCreator().getSnsUrl(),
        creatorMetrics,
        application.getFulfillmentType(),
        application.getStatus(),
        application.getMessage(),
        contact,
        read(application.getAnswersJson(), new TypeReference<>() {}),
        read(application.getAgreementAcceptancesJson(), new TypeReference<>() {}),
        application.getFormVersion(),
        application.getEligibilityRequirementSnapshotJson() == null
            ? null
            : read(application.getEligibilityRequirementSnapshotJson(), new TypeReference<>() {}),
        application.getEligibilityRequirementVersion(),
        application.isIdentityVerificationConfigured(),
        application.isIdentityVerified(),
        application.isEmailVerified(),
        application.isChannelReady(),
        notices,
        application.getSubmittedAt(),
        application.getRespondedAt(),
        application.getCancelledAt(),
        application.getCancellationReason(),
        application.getExperienceConfirmedAt(),
        completedCampaignCount);
  }

  /** 지표 집계 행을 화면용 응답으로 바꾸고, 지표 종류 코드를 한국어 이름으로 바꿉니다. */
  private CampaignParticipantDtos.ChannelMetric metric(
      CampaignApplicationRepository.CreatorMetricSummary metric) {
    return new CampaignParticipantDtos.ChannelMetric(
        metric.getChannelType(),
        metric.getMetricType(),
        switch (metric.getMetricType()) {
          case "FOLLOWER_COUNT" -> "팔로워 수";
          case "AVERAGE_VISITORS" -> "평균 방문자 수";
          case "SUBSCRIBER_COUNT" -> "구독자 수";
          default -> metric.getMetricType();
        },
        metric.getMetricValue(),
        metric.getMeasurementPeriod(),
        metric.getMeasuredAt(),
        com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus.valueOf(
            metric.getVerificationStatus()),
        "UNKNOWN");
  }

  /** 엔티티의 상태 규칙 위반(IllegalStateException)을 409 응답으로 바꿔 화면에 사유를 전달합니다. */
  private CampaignApplicationDtos.Response transition(
      CampaignApplication application, Consumer<CampaignApplication> action) {
    try {
      action.accept(application);
      return response(application);
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
    }
  }

  /** 선정 결과를 참여자에게 알리고 감사 로그에 남깁니다. */
  private void notifyCreatorAndAudit(
      Long actorMemberId,
      CampaignApplication application,
      String title,
      String action,
      String nextStatus,
      String reason) {
    notificationService.send(
        application.getCreator().getMember().getId(),
        "CAMPAIGN_APPLICATION",
        title,
        application.getCampaign().getTitle(),
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        actorMemberId,
        action,
        "CAMPAIGN_APPLICATION",
        application.getId(),
        "{\"status\":\"SUBMITTED\"}",
        "{\"status\":\"" + nextStatus + "\"}",
        reason);
  }

  private String contactValue(
      CampaignApplicationDtos.CreateRequest request,
      Function<CampaignApplicationDtos.ShippingContact, String> getter) {
    return request.shippingContact() == null
        ? null
        : normalize(getter.apply(request.shippingContact()));
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "신청서 데이터를 저장하지 못했습니다.");
    }
  }

  private <T> T read(String json, TypeReference<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JacksonException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "신청서 데이터를 읽지 못했습니다.");
    }
  }
}
