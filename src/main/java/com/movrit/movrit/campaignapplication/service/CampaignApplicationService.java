package com.movrit.movrit.campaignapplication.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.dto.CampaignParticipantDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
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

/**
 * 캠페인 신청의 전 과정을 처리합니다.
 *
 * <p>참여자의 신청 가능 여부 확인과 신청, 선정·미선정, 참여 수락, 체험 완료 확인, 취소, 그리고 참여자·업체·운영자 화면용 목록 조회를 맡습니다. 선정은 캠페인의 선정
 * 방식에 따라 소유 업체(BRAND_DIRECT) 또는 운영자(OPERATOR)만 할 수 있습니다. 상태가 바뀔 때마다 상대방에게 알림을 보내고 감사 로그를 남깁니다.
 *
 * <p>함께 쓰는 도우미: 신청서 양식 저장·조회는 {@link CampaignApplicationFormService}, 신청서 내용 검사는 {@link
 * CampaignApplicationValidator}, 응답 만들기는 {@link CampaignApplicationResponses}, JSON 변환은 {@link
 * CampaignApplicationJson}이 맡습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignApplicationService {
  private final CampaignRepository campaignRepository;
  private final CampaignApplicationFormService forms;
  private final CampaignApplicationValidator validator;
  private final CampaignApplicationRepository applicationRepository;
  private final CreatorService creatorService;
  private final MemberService memberService;
  private final CampaignEligibilityPolicyService eligibilityPolicyService;
  private final MemberChannelMetricService memberChannelMetricService;
  private final SellerProfileService sellerProfileService;
  private final CampaignOwnershipService campaignOwnership;
  private final CampaignApplicationJson json;
  private final CampaignApplicationResponses responses;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;
  private final com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService visitEvidence;
  private final CampaignContentService campaignContentService;
  private final com.movrit.movrit.participation.service.ActivityService activityService;

  /**
   * 로그인한 회원이 이 캠페인에 지금 신청할 수 있는지 판정합니다.
   *
   * <p>모집 기간, 이메일 소유 확인, 캠페인별 채널 지표 조건, 중복 신청 여부를 확인해 막는 사유 목록(reasons)을 돌려줍니다. 사유가 없으면 신청할 수 있습니다.
   * 화면 안내용이며 실제 신청 시 서버가 같은 조건을 다시 검사합니다.
   */
  public CampaignApplicationDtos.EligibilityResponse getEligibility(
      Long memberId, Long campaignId) {
    Campaign campaign = getCampaign(campaignId);
    CampaignApplicationForm form = forms.getFormEntity(campaignId);
    Member member = memberService.getMember(memberId);
    Creator creator = creatorService.findOptionalByMember(memberId).orElse(null);
    CampaignEligibilityPolicyService.Evaluation evaluation =
        eligibilityPolicyService.evaluate(memberId, campaignId);
    List<String> reasons = campaignEligibilityReasons(member, campaign, evaluation);
    boolean alreadyApplied =
        applicationRepository.existsByCampaignIdAndCreatorMemberId(campaignId, memberId);
    if (alreadyApplied) {
      // 선정 발표 전에 스스로 철회한 신청이면 '이미 신청'이 아니라 '철회한 캠페인'으로 알려 줍니다.
      boolean withdrawn =
          applicationRepository
              .existsByCampaignIdAndCreatorMemberIdAndStatusAndCancellationReasonStartingWith(
                  campaignId, memberId, CampaignApplicationStatus.CANCELLED, "[신청 철회]");
      reasons.add(withdrawn ? "ALREADY_WITHDRAWN" : "ALREADY_APPLIED");
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
    CampaignApplicationForm form = forms.getFormEntity(campaignId);
    Member member = memberService.getMember(memberId);
    // 미제출·노쇼 패널티로 신청이 제한된 회원은 막습니다(제한이 끝나는 날짜를 안내).
    activityService.requireCanApply(memberId);

    if (!form.getVersion().equals(request.expectedFormVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "신청 조건이 변경되었습니다. 최신 신청서를 다시 확인해 주세요.");
    }
    CampaignEligibilityPolicyService.Evaluation eligibility = validateEligibility(member, campaign);
    validateExpectedRequirement(request, eligibility.requirement());
    if (applicationRepository.existsByCampaignIdAndCreatorMemberId(campaignId, memberId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
    }
    validator.validateApplication(form, request);
    // 선착순 캠페인은 방문형으로만 받고, 모집 인원이 찼으면 신청 자체를 받지 않습니다(캠페인 행을 잠근 상태라 동시에 신청해도 넘치지 않습니다).
    boolean firstCome = campaign.getSelectionMode() == CampaignSelectionMode.FIRST_COME;
    if (firstCome) {
      if (request.fulfillmentType() != FulfillmentType.VISIT) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선착순 캠페인은 방문형으로만 신청할 수 있습니다.");
      }
      if (countSelected(campaignId) >= campaign.getRecruitmentCount()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "선착순 모집 인원이 모두 찼습니다.");
      }
    }
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
                contactPhone(
                    contactValue(request, CampaignApplicationDtos.ShippingContact::recipientPhone)))
            .postalCode(contactValue(request, CampaignApplicationDtos.ShippingContact::postalCode))
            .address1(contactValue(request, CampaignApplicationDtos.ShippingContact::address1))
            .address2(contactValue(request, CampaignApplicationDtos.ShippingContact::address2))
            .answersJson(json.write(request.answers()))
            .agreementAcceptancesJson(json.write(request.agreementAcceptances()))
            .formVersion(form.getVersion())
            .eligibilityRequirementSnapshotJson(
                eligibility.requirement() == null ? null : json.write(eligibility.requirement()))
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
      // 선착순이면 신청과 동시에 선정(진행 중)으로 바꿉니다.
      if (firstCome) saved.select();
      Long sellerMemberId = campaign.getBrand().getSeller().getMember().getId();
      notificationService.send(
          sellerMemberId,
          "CAMPAIGN_APPLICATION",
          firstCome ? "선착순 신청으로 참여자가 선정되었습니다." : "새 캠페인 신청이 접수되었습니다.",
          creator.getNickname() + "님이 캠페인에 신청하셨습니다.",
          "CAMPAIGN_APPLICATION",
          saved.getId());
      if (firstCome) {
        notificationService.send(
            memberId,
            "CAMPAIGN_APPLICATION",
            "선착순으로 바로 선정되었습니다. 방문 일정을 잡아 주세요.",
            campaign.getTitle(),
            "CAMPAIGN_APPLICATION",
            saved.getId());
      }
      auditLogService.record(
          memberId,
          firstCome ? "CAMPAIGN_APPLICATION_FIRST_COME_SELECTED" : "CAMPAIGN_APPLICATION_SUBMITTED",
          "CAMPAIGN_APPLICATION",
          saved.getId(),
          null,
          "{\"status\":\"" + saved.getStatus() + "\"}",
          null);
      return responses.response(saved);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
    }
  }

  /**
   * 참여자가 선정 전(접수) 신청의 내용을 고칩니다. 신청 메시지·배송지·질문 답변을 바꿀 수 있고, 업체에 알림을 보냅니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 접수 상태가 아니거나 신청서가 바뀌었으면 409, 입력 오류는 400
   */
  @Transactional
  public CampaignApplicationDtos.Response revise(
      Long memberId, Long applicationId, CampaignApplicationDtos.ReviseRequest request) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    if (application.getStatus() != CampaignApplicationStatus.SUBMITTED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "선정 결과가 나온 신청은 고칠 수 없습니다.");
    }
    CampaignApplicationForm form = forms.getFormEntity(application.getCampaign().getId());
    if (!form.getVersion().equals(request.expectedFormVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "신청서가 바뀌었습니다. 최신 신청서를 다시 확인해 주세요.");
    }
    validator.validateRevision(form, application.getFulfillmentType(), request);
    CampaignApplicationDtos.ShippingContact contact = request.shippingContact();
    CampaignApplicationDtos.Response response =
        transition(
            application,
            value ->
                value.revise(
                    request.message().trim(),
                    contact == null ? null : normalize(contact.recipientName()),
                    contact == null ? null : contactPhone(normalize(contact.recipientPhone())),
                    contact == null ? null : normalize(contact.postalCode()),
                    contact == null ? null : normalize(contact.address1()),
                    contact == null ? null : normalize(contact.address2()),
                    json.write(request.answers()),
                    form.getVersion()));
    notificationService.send(
        application.getCampaign().getBrand().getSeller().getMember().getId(),
        "CAMPAIGN_APPLICATION",
        "신청 내용이 수정되었습니다.",
        application.getCreator().getNickname() + "님이 신청 내용을 고쳤습니다.",
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        memberId,
        "CAMPAIGN_APPLICATION_REVISED",
        "CAMPAIGN_APPLICATION",
        application.getId(),
        null,
        "{\"revised\":true}",
        null);
    return response;
  }

  /** 로그인한 참여자의 신청 목록을 최신순으로 조회합니다. */
  public List<CampaignApplicationDtos.Response> findMine(Long memberId) {
    return applicationRepository.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(responses::response)
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
                  responses.response(application),
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
                      .add(responses.metric(metric)));
    }
    return applications.stream()
        .map(
            application ->
                responses.sellerResponse(
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
        .map(responses::sellerResponse)
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
    if (countSelected(lockedCampaign.getId()) >= lockedCampaign.getRecruitmentCount()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 모집 인원이 모두 선정되었습니다.");
    }
    CampaignApplicationDtos.Response response =
        transition(application, CampaignApplication::select);
    // 선정과 동시에 참여가 확정되므로 참여자에게는 바로 체험을 시작하도록 안내합니다.
    notifyCreatorAndAudit(
        actorMemberId,
        application,
        "캠페인에 선정되었습니다. 체험 후 콘텐츠를 제출해 주세요.",
        "CAMPAIGN_APPLICATION_SELECTED",
        "ACTIVE",
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

  // ───────── 선정 편집(업체·운영자) ─────────

  /** 선정 편집 종류입니다. 상태 변경, 참여자 알림 제목, 감사 기록 이름을 함께 묶습니다. */
  private enum ManageAction {
    UNSELECT("선정이 취소되었습니다.", "CAMPAIGN_APPLICATION_UNSELECTED", "SUBMITTED"),
    RESTORE("신청이 다시 검토 대상이 되었습니다.", "CAMPAIGN_APPLICATION_RESTORED", "SUBMITTED"),
    CANCEL("업체 사정으로 캠페인 참여가 취소되었습니다.", "CAMPAIGN_APPLICATION_MANAGER_CANCELLED", "CANCELLED");

    private final String notice;
    private final String auditAction;
    private final String nextStatus;

    ManageAction(String notice, String auditAction, String nextStatus) {
      this.notice = notice;
      this.auditAction = auditAction;
      this.nextStatus = nextStatus;
    }
  }

  /**
   * 업체가 선정을 편집합니다(업체 직접 선정 캠페인).
   *
   * <ul>
   *   <li>UNSELECT: 체험 완료 확인 전인 선정을 취소하고 다시 접수 상태로 돌립니다.
   *   <li>RESTORE: 미선정을 되돌려 다시 접수 상태로 검토합니다.
   *   <li>CANCEL: 참여자 사정 등으로 선정·참여 중인 신청을 취소합니다(사유 필수).
   * </ul>
   *
   * <p>취소·선정 취소로 자리가 비면 다른 신청자를 새로 선정할 수 있습니다. 참여자에게 알림을 보내고 감사 기록을 남깁니다.
   */
  @Transactional
  public CampaignApplicationDtos.Response manage(
      Long memberId, Long applicationId, String action, String reason) {
    CampaignApplication application = getSellerApplication(memberId, applicationId);
    requireSelectionMode(application, CampaignSelectionMode.BRAND_DIRECT);
    return manageApplication(memberId, application, parseAction(action), reason);
  }

  /** 운영자가 선정을 편집합니다(운영자 선정 캠페인). 규칙은 업체 편집과 같습니다. */
  @Transactional
  public CampaignApplicationDtos.Response operatorManage(
      Long adminMemberId, Long applicationId, String action, String reason) {
    memberService.getAdmin(adminMemberId);
    CampaignApplication application = getApplication(applicationId);
    requireSelectionMode(application, CampaignSelectionMode.OPERATOR);
    return manageApplication(adminMemberId, application, parseAction(action), reason);
  }

  /** 선정 편집 공통 처리입니다. 캠페인을 잠가 선정 인원 계산과 겹치지 않게 합니다. */
  private CampaignApplicationDtos.Response manageApplication(
      Long actorMemberId, CampaignApplication application, ManageAction action, String reason) {
    campaignRepository
        .findByIdForUpdate(application.getCampaign().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    String cleanReason = reason == null || reason.isBlank() ? null : reason.trim();
    if (action == ManageAction.CANCEL && cleanReason == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "참여 취소 사유를 입력해 주세요.");
    }
    CampaignApplicationStatus previous = application.getStatus();
    CampaignApplicationDtos.Response response =
        transition(
            application,
            value -> {
              switch (action) {
                case UNSELECT -> value.unselect();
                case RESTORE -> value.restoreToReview();
                case CANCEL -> value.cancelByManager(cleanReason);
              }
            });
    notificationService.send(
        application.getCreator().getMember().getId(),
        "CAMPAIGN_APPLICATION",
        action.notice,
        application.getCampaign().getTitle() + (cleanReason == null ? "" : " · 사유: " + cleanReason),
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        actorMemberId,
        action.auditAction,
        "CAMPAIGN_APPLICATION",
        application.getId(),
        "{\"status\":\"" + previous + "\"}",
        "{\"status\":\"" + action.nextStatus + "\"}",
        cleanReason);
    return response;
  }

  /** 화면에서 보낸 편집 이름(unselect·restore·cancel)을 편집 종류로 바꿉니다. */
  private static ManageAction parseAction(String action) {
    return switch (action == null ? "" : action) {
      case "unselect" -> ManageAction.UNSELECT;
      case "restore" -> ManageAction.RESTORE;
      case "cancel" -> ManageAction.CANCEL;
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 선정 편집입니다.");
    };
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
   * 예전 참여자 취소 주소입니다. 상태를 바꾸지 않고 409로 새 기능을 안내합니다.
   *
   * <p>참여자 취소는 참여 조정 API(신청 철회·참여 취소·협의 취소, {@code ParticipationChangeController})로 옮겼습니다. 이전 화면이나
   * 외부 호출이 이 주소를 부르더라도 취소 횟수 규칙을 건너뛰지 않도록 막아 둡니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 본인 신청이면 항상 409
   */
  @Transactional(readOnly = true)
  public CampaignApplicationDtos.Response cancel(
      Long memberId, Long applicationId, CampaignApplicationDtos.CancelRequest request) {
    applicationRepository
        .findByIdAndCreatorMemberId(applicationId, memberId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    throw new ResponseStatusException(
        HttpStatus.CONFLICT, "내 캠페인 화면의 '신청 철회' 또는 '참여 취소'를 이용해 주세요.");
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
    // 구매평 캠페인은 '체험 완료 확인' 대신 구매 인증(주문번호·캡처)을 제출해야 다음 단계로 넘어갑니다.
    if (application.getFulfillmentType() == FulfillmentType.PURCHASE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "구매평 캠페인은 구매 인증을 제출해 주세요.");
    }
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

  /** 이미 선정되어 진행 중이거나 끝난 인원을 셉니다(모집 인원 비교용). 취소·미선정은 세지 않습니다. */
  private long countSelected(Long campaignId) {
    return applicationRepository.countByCampaignIdAndStatusIn(
        campaignId,
        List.of(
            CampaignApplicationStatus.SELECTED,
            CampaignApplicationStatus.ACTIVE,
            CampaignApplicationStatus.CONTENT_SUBMITTED,
            CampaignApplicationStatus.REVISION_REQUESTED,
            CampaignApplicationStatus.COMPLETED));
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

  /** 엔티티의 상태 규칙 위반(IllegalStateException)을 409 응답으로 바꿔 화면에 사유를 전달합니다. */
  private CampaignApplicationDtos.Response transition(
      CampaignApplication application, Consumer<CampaignApplication> action) {
    try {
      action.accept(application);
      return responses.response(application);
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

  /**
   * 배송 연락처를 010-1234-5678·02-123-4567 모양으로 맞춰 저장합니다. 업체 배송 화면과 엑셀에 같은 모양으로 보이게 하려는 것이며, 숫자·하이픈·공백
   * 외의 글자가 있거나 자릿수가 전화번호와 맞지 않으면 입력한 그대로 둡니다.
   */
  static String contactPhone(String value) {
    if (value == null) return null;
    String text = value.trim();
    if (!text.matches("[0-9\\-\\s]+")) return text;
    String d = text.replaceAll("\\D", "");
    if (d.startsWith("02") && (d.length() == 9 || d.length() == 10)) {
      int middle = d.length() == 10 ? 4 : 3;
      return "02-" + d.substring(2, 2 + middle) + "-" + d.substring(2 + middle);
    }
    if (!d.startsWith("02") && (d.length() == 10 || d.length() == 11)) {
      int middle = d.length() == 11 ? 4 : 3;
      return d.substring(0, 3) + "-" + d.substring(3, 3 + middle) + "-" + d.substring(3 + middle);
    }
    return text;
  }
}
