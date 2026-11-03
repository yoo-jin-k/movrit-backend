package com.movrit.movrit.participation.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.participation.dto.ParticipationChangeDtos;
import com.movrit.movrit.participation.entity.CampaignCancelRequest;
import com.movrit.movrit.participation.entity.CampaignExtensionRequest;
import com.movrit.movrit.participation.entity.MemberCancelCount;
import com.movrit.movrit.participation.repository.CampaignCancelRequestRepository;
import com.movrit.movrit.participation.repository.CampaignExtensionRequestRepository;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 참여 조정: 참여자가 신청을 거두거나 참여를 그만두는 일, 기간을 늘리는 일을 처리합니다.
 *
 * <ul>
 *   <li>신청 철회: 선정 발표 전 접수 상태에서 참여자가 바로 거둡니다. 취소 횟수를 남기지 않습니다(같은 캠페인에 다시 신청할 수는 없습니다).
 *   <li>참여 취소(단순 취소): 선정된 뒤 콘텐츠를 내기 전 참여자가 바로 취소합니다. 취소 횟수 1회가 남습니다.
 *   <li>협의 취소: 업체와 합의했거나 업체 사정일 때 사유와 증빙을 올리면 운영자가 확인합니다. 승인되면 취소 횟수 없이 취소합니다.
 *   <li>기간 연장: 참여자가 7·14일 연장을 요청하고 캠페인 업체(또는 운영자)가 승인하면 이 신청의 제출 마감만 늦춥니다. 신청당 1회입니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationChangeService {
  private final CampaignApplicationRepository applicationRepository;
  private final CampaignCancelRequestRepository cancelRequestRepository;
  private final CampaignExtensionRequestRepository extensionRepository;
  private final CancelCountService cancelCountService;
  private final CancelEvidenceStorage evidenceStorage;
  private final CampaignOwnershipService campaignOwnership;
  private final MemberService memberService;
  private final MemberRepository memberRepository;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  /** 참여자 본인 신청에서 지금 할 수 있는 일과 진행 중인 요청을 돌려줍니다. */
  public ParticipationChangeDtos.Status status(Long memberId, Long applicationId) {
    CampaignApplication application = getOwnApplication(memberId, applicationId);
    return statusOf(application, memberId);
  }

  /**
   * 신청을 철회합니다(선정 발표 전). 취소 횟수는 남기지 않고 업체에 알립니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 선정 발표가 지났거나 접수 상태가 아니면 409
   */
  @Transactional
  public ParticipationChangeDtos.Status withdraw(
      Long memberId, Long applicationId, ParticipationChangeDtos.WithdrawRequest request) {
    CampaignApplication application = getOwnApplication(memberId, applicationId);
    if (!canWithdraw(application, LocalDateTime.now())) {
      throw conflict("선정 결과가 나오기 전인 신청만 철회할 수 있습니다.");
    }
    String reason = blankToDefault(request == null ? null : request.reason(), "참여자 사정");
    apply(application, value -> value.withdraw(reason));
    notifySeller(application, "참여자가 신청을 철회했습니다.");
    audit(memberId, "CAMPAIGN_APPLICATION_WITHDRAWN", application, "SUBMITTED", reason);
    return statusOf(application, memberId);
  }

  /**
   * 참여를 바로 취소합니다(단순 취소). 취소 횟수 1회를 남기고 업체에 알립니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 콘텐츠 제출 뒤이거나 협의 취소를 확인 중이면 409
   */
  @Transactional
  public ParticipationChangeDtos.Status selfCancel(
      Long memberId, Long applicationId, ParticipationChangeDtos.SelfCancelRequest request) {
    CampaignApplication application = getOwnApplication(memberId, applicationId);
    requireNoOpenCancelRequest(application);
    String reason = request.reason().trim();
    apply(application, value -> value.cancelForParticipant("참여자 취소", reason));
    cancelCountService.record(memberId, applicationId, MemberCancelCount.SELF_CANCEL);
    notifySeller(application, "참여자가 참여를 취소했습니다.");
    audit(memberId, "CAMPAIGN_APPLICATION_SELF_CANCELLED", application, "ACTIVE", reason);
    return statusOf(application, memberId);
  }

  /**
   * 협의 취소를 요청합니다. 사유와 증빙 이미지(JPG·PNG, 5MB)를 받아 운영자에게 알립니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 콘텐츠 제출 뒤이거나 이미 확인 중이면 409, 이미지 오류는 400
   */
  @Transactional
  public ParticipationChangeDtos.Status requestAgreedCancel(
      Long memberId, Long applicationId, String reason, MultipartFile evidence) {
    CampaignApplication application = getOwnApplication(memberId, applicationId);
    if (!application.isInProgressBeforeSubmission()) {
      throw conflict("콘텐츠를 내기 전인 선정·진행 중 참여만 협의 취소를 요청할 수 있습니다.");
    }
    requireNoOpenCancelRequest(application);
    String trimmed = reason == null ? "" : reason.trim();
    if (trimmed.isEmpty() || trimmed.length() > 500) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "협의 내용을 500자 안으로 적어 주세요.");
    }
    String key = evidenceStorage.store(evidence);
    try {
      cancelRequestRepository.saveAndFlush(
          new CampaignCancelRequest(applicationId, memberId, trimmed, key));
    } catch (RuntimeException exception) {
      // 같은 신청에 동시에 두 번 요청하면 하나만 남기고, 저장하지 못한 이미지는 지웁니다.
      evidenceStorage.delete(key);
      throw conflict("이미 협의 취소를 확인하고 있습니다.");
    }
    for (var admin :
        memberRepository.findAllByRoleAndStatus(MemberRole.ADMIN, MemberStatus.ACTIVE)) {
      notificationService.send(
          admin.getId(),
          "CAMPAIGN_CANCEL_REQUEST",
          "협의 취소 요청이 들어왔습니다.",
          application.getCampaign().getTitle(),
          // 운영자 알림은 '협의 취소 확인' 화면으로 이동합니다.
          "ADMIN_CANCEL_REQUEST",
          applicationId);
    }
    audit(memberId, "CAMPAIGN_CANCEL_REQUESTED", application, "ACTIVE", trimmed);
    return statusOf(application, memberId);
  }

  /** 운영자 협의 취소 목록입니다(확인 중인 요청 먼저, 최근 200건). */
  public List<ParticipationChangeDtos.CancelRequest> adminCancelRequests(Long adminMemberId) {
    memberService.getAdmin(adminMemberId);
    return cancelRequestRepository.findTop200ByOrderByStatusDescCreatedAtAsc().stream()
        .map(this::view)
        .toList();
  }

  /**
   * 운영자가 협의 취소를 승인하거나 반려합니다. 승인하면 취소 횟수 없이 참여를 취소하고 참여자·업체에 알립니다.
   *
   * @throws ResponseStatusException 운영자가 아니면 403, 이미 처리했거나 참여 상태가 바뀌었으면 409
   */
  @Transactional
  public ParticipationChangeDtos.CancelRequest decideCancelRequest(
      Long adminMemberId, Long requestId, ParticipationChangeDtos.Decision decision) {
    memberService.getAdmin(adminMemberId);
    CampaignCancelRequest request =
        cancelRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."));
    boolean approve = Boolean.TRUE.equals(decision.approve());
    String note = blankToDefault(decision.note(), null);
    if (!approve && note == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반려 사유를 적어 주세요.");
    }
    CampaignApplication application = getApplication(request.getApplicationId());
    try {
      request.decide(approve, adminMemberId, note);
    } catch (IllegalStateException exception) {
      throw conflict(exception.getMessage());
    }
    if (approve) {
      apply(application, value -> value.cancelForParticipant("협의 취소", request.getReason()));
      notifySeller(application, "운영자가 협의 취소를 승인했습니다.");
    }
    notificationService.send(
        request.getMemberId(),
        "CAMPAIGN_CANCEL_REQUEST",
        approve ? "협의 취소가 승인되었습니다." : "협의 취소가 반려되었습니다.",
        application.getCampaign().getTitle() + (note == null ? "" : " · " + note),
        "CAMPAIGN_APPLICATION",
        application.getId());
    audit(
        adminMemberId,
        approve ? "CAMPAIGN_CANCEL_APPROVED" : "CAMPAIGN_CANCEL_REJECTED",
        application,
        "ACTIVE",
        note);
    return view(request);
  }

  /** 협의 취소 증빙 이미지입니다. 요청한 참여자·캠페인 업체·운영자만 볼 수 있습니다(그 외 404). */
  public Evidence cancelEvidence(Long viewerId, Long requestId) {
    CampaignCancelRequest request =
        cancelRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."));
    CampaignApplication application = getApplication(request.getApplicationId());
    boolean allowed =
        request.getMemberId().equals(viewerId)
            || memberService.getMember(viewerId).isAdmin()
            || isOwner(viewerId, application);
    if (!allowed) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다.");
    return new Evidence(
        evidenceStorage.load(request.getEvidenceKey()),
        CancelEvidenceStorage.contentType(request.getEvidenceKey()));
  }

  /**
   * 기간 연장을 요청합니다(7일 또는 14일, 신청당 1회). 캠페인 업체에 알립니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 일수 오류는 400, 이미 요청했거나 마감이 지났거나 제출 뒤면 409
   */
  @Transactional
  public ParticipationChangeDtos.Status requestExtension(
      Long memberId, Long applicationId, ParticipationChangeDtos.ExtensionRequest request) {
    CampaignApplication application = getOwnApplication(memberId, applicationId);
    if (!CampaignExtensionRequest.ALLOWED_DAYS.contains(request.days())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연장은 7일 또는 14일만 요청할 수 있습니다.");
    }
    if (extensionRepository.existsById(applicationId)) {
      throw conflict("기간 연장은 신청마다 한 번만 요청할 수 있습니다.");
    }
    if (!canRequestExtension(application, LocalDateTime.now())) {
      throw conflict("콘텐츠 제출 마감 전, 콘텐츠를 내기 전인 진행 중 참여만 연장을 요청할 수 있습니다.");
    }
    String reason = request.reason().trim();
    extensionRepository.save(
        new CampaignExtensionRequest(
            applicationId, memberId, request.days(), reason, application.contentDeadline()));
    notifySeller(application, "참여자가 기간 연장(" + request.days() + "일)을 요청했습니다.");
    audit(memberId, "CAMPAIGN_EXTENSION_REQUESTED", application, "ACTIVE", reason);
    return statusOf(application, memberId);
  }

  /** 업체가 운영하는 캠페인의 기간 연장 요청 목록입니다. */
  public List<ParticipationChangeDtos.Extension> sellerExtensions(Long sellerMemberId) {
    return extensionRepository.findAllForSeller(sellerMemberId).stream().map(this::view).toList();
  }

  /**
   * 캠페인 업체(또는 운영자)가 기간 연장을 승인하거나 반려합니다. 승인하면 이 신청의 제출 마감을 요청한 일수만큼 늦추고 참여자에게 알립니다.
   *
   * @throws ResponseStatusException 권한이 없으면 403/404, 이미 처리했거나 참여 상태가 바뀌었으면 409, 반려 사유가 없으면 400
   */
  @Transactional
  public ParticipationChangeDtos.Extension decideExtension(
      Long deciderMemberId, Long applicationId, ParticipationChangeDtos.Decision decision) {
    CampaignApplication application = getApplication(applicationId);
    requireManager(deciderMemberId, application);
    CampaignExtensionRequest request =
        extensionRepository
            .findById(applicationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "연장 요청을 찾을 수 없습니다."));
    boolean approve = Boolean.TRUE.equals(decision.approve());
    String note = blankToDefault(decision.note(), null);
    if (!approve && note == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반려 사유를 적어 주세요.");
    }
    try {
      request.decide(approve, deciderMemberId, note);
      // 체험 기간과 콘텐츠 제출 마감을 함께 늘립니다(이 신청만).
      if (approve) application.extendPeriod(request.getDays(), request.extendedDeadline());
    } catch (IllegalStateException exception) {
      throw conflict(exception.getMessage());
    }
    notificationService.send(
        application.getCreator().getMember().getId(),
        "CAMPAIGN_EXTENSION",
        approve
            ? "기간 연장이 승인되었습니다(" + request.getDays() + "일). 체험 기간과 콘텐츠 마감이 함께 늘어났습니다."
            : "기간 연장이 반려되었습니다.",
        application.getCampaign().getTitle() + (note == null ? "" : " · " + note),
        "CAMPAIGN_APPLICATION",
        applicationId);
    audit(
        deciderMemberId,
        approve ? "CAMPAIGN_EXTENSION_APPROVED" : "CAMPAIGN_EXTENSION_REJECTED",
        application,
        "ACTIVE",
        note);
    return view(request);
  }

  /** 선정 발표 전 접수 상태인지 확인합니다. 선정 발표일이 없으면 접수 상태인 동안 철회할 수 있습니다. */
  static boolean canWithdraw(CampaignApplication application, LocalDateTime now) {
    LocalDateTime announcedAt = application.getCampaign().getSelectionAnnouncedAt();
    return application.getStatus() == CampaignApplicationStatus.SUBMITTED
        && (announcedAt == null || now.isBefore(announcedAt));
  }

  /** 콘텐츠를 내기 전인 진행 중 참여이고, 제출 마감 전인지 확인합니다. */
  static boolean canRequestExtension(CampaignApplication application, LocalDateTime now) {
    LocalDateTime deadline = application.contentDeadline();
    return application.isInProgressBeforeSubmission() && deadline != null && now.isBefore(deadline);
  }

  private ParticipationChangeDtos.Status statusOf(CampaignApplication application, Long memberId) {
    LocalDateTime now = LocalDateTime.now();
    Long id = application.getId();
    ParticipationChangeDtos.Extension extension =
        extensionRepository.findById(id).map(this::view).orElse(null);
    ParticipationChangeDtos.CancelRequest cancelRequest =
        cancelRequestRepository
            .findFirstByApplicationIdOrderByIdDesc(id)
            .map(this::view)
            .orElse(null);
    boolean openCancel =
        cancelRequest != null && CampaignCancelRequest.REQUESTED.equals(cancelRequest.status());
    return new ParticipationChangeDtos.Status(
        id,
        application.getStatus(),
        canWithdraw(application, now),
        application.isInProgressBeforeSubmission() && !openCancel,
        extension == null && canRequestExtension(application, now),
        application.contentDeadline(),
        application.getContentDeadlineExtendedUntil(),
        application.getExperienceExtendedUntil(),
        extension,
        cancelRequest,
        cancelCountService.activeCount(memberId),
        CancelCountService.PENALTY_EVERY);
  }

  private ParticipationChangeDtos.Extension view(CampaignExtensionRequest request) {
    CampaignApplication application = getApplication(request.getApplicationId());
    return new ParticipationChangeDtos.Extension(
        request.getApplicationId(),
        application.getCampaign().getId(),
        application.getCampaign().getTitle(),
        application.getCreator().getNickname(),
        request.getDays(),
        request.getReason(),
        request.getPreviousDeadline(),
        request.extendedDeadline(),
        request.getStatus(),
        request.getDecisionNote(),
        request.getCreatedAt(),
        request.getDecidedAt());
  }

  private ParticipationChangeDtos.CancelRequest view(CampaignCancelRequest request) {
    CampaignApplication application = getApplication(request.getApplicationId());
    return new ParticipationChangeDtos.CancelRequest(
        request.getId(),
        request.getApplicationId(),
        application.getCampaign().getId(),
        application.getCampaign().getTitle(),
        application.getCampaign().getBrand().getName(),
        application.getCreator().getNickname(),
        request.getReason(),
        request.getStatus(),
        request.getDecisionNote(),
        request.getCreatedAt(),
        request.getDecidedAt());
  }

  private void requireNoOpenCancelRequest(CampaignApplication application) {
    if (cancelRequestRepository.existsByApplicationIdAndStatus(
        application.getId(), CampaignCancelRequest.REQUESTED)) {
      throw conflict("협의 취소를 확인하고 있습니다. 결과가 나온 뒤 다시 시도해 주세요.");
    }
  }

  /** 엔티티 상태 규칙 위반을 409로 바꿉니다. */
  private static void apply(CampaignApplication application, Consumer<CampaignApplication> action) {
    try {
      action.accept(application);
    } catch (IllegalStateException exception) {
      throw conflict(exception.getMessage());
    }
  }

  private void notifySeller(CampaignApplication application, String title) {
    notificationService.send(
        application.getCampaign().getBrand().getSeller().getMember().getId(),
        "CAMPAIGN_APPLICATION",
        title,
        application.getCreator().getNickname() + " · " + application.getCampaign().getTitle(),
        "CAMPAIGN_APPLICATION",
        application.getId());
  }

  private void audit(
      Long actorId, String action, CampaignApplication application, String before, String reason) {
    auditLogService.record(
        actorId,
        action,
        "CAMPAIGN_APPLICATION",
        application.getId(),
        "{\"status\":\"" + before + "\"}",
        "{\"status\":\"" + application.getStatus() + "\"}",
        reason);
  }

  private boolean isOwner(Long viewerId, CampaignApplication application) {
    try {
      campaignOwnership.requireOwner(viewerId, application.getCampaign());
      return true;
    } catch (ResponseStatusException exception) {
      return false;
    }
  }

  private void requireManager(Long viewerId, CampaignApplication application) {
    if (memberService.getMember(viewerId).isAdmin()) return;
    campaignOwnership.requireOwner(viewerId, application.getCampaign());
  }

  private CampaignApplication getOwnApplication(Long memberId, Long applicationId) {
    return applicationRepository
        .findByIdAndCreatorMemberId(applicationId, memberId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
  }

  private CampaignApplication getApplication(Long applicationId) {
    return applicationRepository
        .findById(applicationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
  }

  private static String blankToDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  /** 증빙 이미지 파일과 응답 형식입니다. */
  public record Evidence(Path path, String contentType) {}
}
