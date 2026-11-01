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
    CampaignApplicationForm form = forms.getFormEntity(campaignId);
    Member member = memberService.getMember(memberId);

    if (!form.getVersion().equals(request.expectedFormVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "신청 조건이 변경되었습니다. 최신 신청서를 다시 확인해 주세요.");
    }
    CampaignEligibilityPolicyService.Evaluation eligibility = validateEligibility(member, campaign);
    validateExpectedRequirement(request, eligibility.requirement());
    if (applicationRepository.existsByCampaignIdAndCreatorMemberId(campaignId, memberId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
    }
    validator.validateApplication(form, request);
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
      return responses.response(saved);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
    }
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
   * 참여자 본인 취소 요청을 거절합니다.
   *
   * <p>캠페인 신청은 한 번 하면 본인이 취소할 수 없습니다. 참여가 어려운 사정이 생기면 업체에 문의하고, 업체가 선정 편집의 '참여 취소'로 처리합니다. 이전 화면이나
   * 외부 호출이 이 주소를 부르더라도 상태가 바뀌지 않도록 409로 안내합니다.
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
        HttpStatus.CONFLICT, "캠페인 신청은 직접 취소할 수 없습니다. 참여가 어려우면 업체 담당자에게 문의해 주세요.");
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
}
