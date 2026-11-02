package com.movrit.movrit.participation.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.participation.dto.ParticipationDtos;
import com.movrit.movrit.participation.entity.MemberPenalty;
import com.movrit.movrit.participation.entity.ParticipantRating;
import com.movrit.movrit.participation.repository.MemberPenaltyRepository;
import com.movrit.movrit.participation.repository.ParticipantRatingRepository;
import com.movrit.movrit.participation.repository.VisitReservationRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 활동 등급·패널티·참여자 평가를 처리합니다.
 *
 * <ul>
 *   <li>등급: 완료한 캠페인 수로 정합니다({@link ActivityGrades}).
 *   <li>패널티: 업체가 미제출(콘텐츠 마감 경과)·노쇼(확정한 방문 일시 경과)를 기록하면 그 참여는 취소되고 참여자에게 알립니다.
 *   <li>신청 제한: 최근 90일 동안 유효한 패널티가 2회 이상이면 마지막 패널티일로부터 30일 동안 새 캠페인에 신청할 수 없습니다. 운영자가 패널티를 취소하면 계산에서
 *       빠집니다.
 *   <li>평가: 완료한 참여자에게 업체가 1~5점과 메모를 남깁니다. 점수 평균은 다른 업체에게 보이고, 메모는 남긴 업체만 봅니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ActivityService {
  static final int WINDOW_DAYS = 90;
  static final int PENALTY_LIMIT = 2;
  static final int RESTRICT_DAYS = 30;

  private final CampaignApplicationRepository applicationRepository;
  private final MemberPenaltyRepository penaltyRepository;
  private final ParticipantRatingRepository ratingRepository;
  private final VisitReservationRepository reservationRepository;
  private final CampaignOwnershipService campaignOwnership;
  private final MemberService memberService;
  private final MemberRepository memberRepository;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  /** 참여자 본인의 등급·패널티입니다(업체 평가 점수·메모는 내려주지 않습니다). */
  @Transactional(readOnly = true)
  public ParticipationDtos.Activity myActivity(Long memberId) {
    return activity(memberId, false);
  }

  /**
   * 업체 참여자 상세용 활동 정보입니다. 그 신청이 속한 캠페인의 업체 또는 운영자만 볼 수 있습니다.
   *
   * @throws ResponseStatusException 볼 권한이 없으면 403/404
   */
  @Transactional(readOnly = true)
  public ParticipationDtos.ParticipantActivity participantActivity(
      Long viewerId, Long applicationId) {
    CampaignApplication application = getApplication(applicationId);
    requireManager(viewerId, application);
    ParticipantRating mine = ratingRepository.findById(applicationId).orElse(null);
    return new ParticipationDtos.ParticipantActivity(
        activity(application.getCreator().getMember().getId(), true),
        mine == null ? null : mine.getRating(),
        mine == null ? null : mine.getMemo());
  }

  /**
   * 신청 전에 신청 제한 여부를 확인합니다.
   *
   * @throws ResponseStatusException 제한 중이면 409(제한이 끝나는 날짜 안내)
   */
  @Transactional(readOnly = true)
  public void requireCanApply(Long memberId) {
    LocalDateTime until =
        restrictedUntil(penaltyRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId));
    if (until != null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "미제출·노쇼 패널티가 쌓여 " + until.toLocalDate() + "까지 새 캠페인에 신청할 수 없습니다.");
    }
  }

  /**
   * 업체가 패널티를 기록합니다. 기록하면 그 참여를 취소하고 참여자에게 알립니다.
   *
   * <ul>
   *   <li>NO_SUBMIT: 진행 중(또는 수정 요청) 신청이고 콘텐츠 제출 마감이 지났을 때
   *   <li>NO_SHOW: 방문형 진행 중 신청이고, 확정한 방문 예약 일시가 지났는데 방문 완료 확인이 없을 때
   * </ul>
   *
   * @throws ResponseStatusException 권한이 없으면 403/404, 조건이 맞지 않거나 이미 기록했으면 409
   */
  @Transactional
  public ParticipationDtos.Penalty recordPenalty(
      Long sellerMemberId, Long applicationId, ParticipationDtos.PenaltyRequest request) {
    CampaignApplication application = getApplication(applicationId);
    campaignOwnership.requireOwner(sellerMemberId, application.getCampaign());
    String type = request.type().trim();
    LocalDateTime now = LocalDateTime.now();
    if (MemberPenalty.NO_SUBMIT.equals(type)) {
      boolean inProgress =
          application.getStatus() == CampaignApplicationStatus.ACTIVE
              || application.getStatus() == CampaignApplicationStatus.REVISION_REQUESTED;
      LocalDateTime deadline = application.getCampaign().getContentSubmissionEndAt();
      if (!inProgress || deadline == null || !now.isAfter(deadline)) {
        throw conflict("콘텐츠 제출 마감이 지났는데 제출하지 않은 참여자에게만 미제출을 기록할 수 있습니다.");
      }
    } else if (MemberPenalty.NO_SHOW.equals(type)) {
      boolean visitWaiting =
          application.getFulfillmentType() == FulfillmentType.VISIT
              && application.getStatus() == CampaignApplicationStatus.ACTIVE
              && application.getExperienceConfirmedAt() == null;
      boolean visitPassed =
          reservationRepository
              .findById(applicationId)
              .filter(reservation -> reservation.isConfirmed())
              .map(reservation -> now.isAfter(reservation.getVisitAt()))
              .orElse(false);
      if (!visitWaiting || !visitPassed) {
        throw conflict("확정한 방문 일시가 지났는데 방문 확인이 없는 참여자에게만 노쇼를 기록할 수 있습니다.");
      }
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "패널티 종류를 확인해 주세요.");
    }
    if (penaltyRepository.existsByApplicationId(applicationId)) {
      throw conflict("이미 패널티를 기록한 참여입니다.");
    }
    String label = MemberPenalty.NO_SUBMIT.equals(type) ? "미제출" : "노쇼";
    String reason = request.reason().trim();
    Long memberId = application.getCreator().getMember().getId();
    MemberPenalty penalty =
        penaltyRepository.save(
            new MemberPenalty(memberId, applicationId, type, reason, sellerMemberId));
    application.cancelByManager("[패널티 " + label + "] " + reason);
    notificationService.send(
        memberId,
        "CAMPAIGN_PENALTY",
        label + " 패널티가 기록되었습니다.",
        application.getCampaign().getTitle() + " · " + reason,
        "CAMPAIGN_APPLICATION",
        applicationId);
    auditLogService.record(
        sellerMemberId,
        "MEMBER_PENALTY_RECORDED",
        "CAMPAIGN_APPLICATION",
        applicationId,
        null,
        "{\"type\":\"" + type + "\"}",
        reason);
    return ParticipationDtos.Penalty.from(
        penalty, application.getCreator().getNickname(), application.getCampaign().getTitle());
  }

  /**
   * 업체가 완료한 참여자를 평가합니다(다시 저장하면 고쳐집니다).
   *
   * @throws ResponseStatusException 권한이 없으면 403/404, 완료 전이면 409
   */
  @Transactional
  public ParticipationDtos.ParticipantActivity rate(
      Long sellerMemberId, Long applicationId, ParticipationDtos.RatingRequest request) {
    CampaignApplication application = getApplication(applicationId);
    campaignOwnership.requireOwner(sellerMemberId, application.getCampaign());
    if (application.getStatus() != CampaignApplicationStatus.COMPLETED) {
      throw conflict("캠페인을 완료한 참여자만 평가할 수 있습니다.");
    }
    Long memberId = application.getCreator().getMember().getId();
    ParticipantRating rating =
        ratingRepository
            .findById(applicationId)
            .orElseGet(() -> new ParticipantRating(applicationId, memberId));
    String memo =
        request.memo() == null || request.memo().isBlank() ? null : request.memo().strip();
    rating.update(request.rating(), memo, sellerMemberId);
    ratingRepository.save(rating);
    return new ParticipationDtos.ParticipantActivity(
        activity(memberId, true), rating.getRating(), rating.getMemo());
  }

  /** 운영자용 최근 패널티 목록(최대 200건)입니다. */
  @Transactional(readOnly = true)
  public List<ParticipationDtos.Penalty> adminPenalties(Long adminMemberId) {
    requireAdmin(adminMemberId);
    return penaltyRepository.findTop200ByOrderByCreatedAtDesc().stream().map(this::view).toList();
  }

  /**
   * 운영자가 패널티를 취소합니다. 참여 취소는 되돌리지 않고, 등급·신청 제한 계산에서만 빠집니다.
   *
   * @throws ResponseStatusException 운영자가 아니면 403, 이미 취소했으면 409
   */
  @Transactional
  public ParticipationDtos.Penalty revoke(
      Long adminMemberId, Long penaltyId, ParticipationDtos.ReasonRequest request) {
    requireAdmin(adminMemberId);
    MemberPenalty penalty =
        penaltyRepository
            .findById(penaltyId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "패널티를 찾을 수 없습니다."));
    try {
      penalty.revoke(adminMemberId, request.reason().trim());
    } catch (IllegalStateException exception) {
      throw conflict(exception.getMessage());
    }
    notificationService.send(
        penalty.getMemberId(),
        "CAMPAIGN_PENALTY",
        "패널티가 취소되었습니다.",
        request.reason().trim(),
        "CAMPAIGN_APPLICATION",
        penalty.getApplicationId());
    auditLogService.record(
        adminMemberId,
        "MEMBER_PENALTY_REVOKED",
        "MEMBER_PENALTY",
        penaltyId,
        "{\"active\":true}",
        "{\"active\":false}",
        request.reason().trim());
    return view(penalty);
  }

  /** 등급·패널티·평가를 모읍니다. withRatings가 false면(참여자 본인 화면) 평가 점수를 내려주지 않습니다. */
  private ParticipationDtos.Activity activity(Long memberId, boolean withRatings) {
    long completed =
        applicationRepository.countByCreatorMemberIdAndStatusIn(
            memberId, List.of(CampaignApplicationStatus.COMPLETED));
    ActivityGrades.Grade grade = ActivityGrades.of(completed);
    List<MemberPenalty> penalties =
        penaltyRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);
    List<ParticipantRating> ratings =
        withRatings ? ratingRepository.findAllByMemberId(memberId) : List.of();
    Double average =
        ratings.isEmpty()
            ? null
            : Math.round(
                    ratings.stream().mapToInt(ParticipantRating::getRating).average().orElse(0)
                        * 10)
                / 10.0;
    return new ParticipationDtos.Activity(
        grade.code(),
        grade.label(),
        grade.nextLabel(),
        grade.remainingToNext(),
        completed,
        penalties.stream().filter(MemberPenalty::isActive).count(),
        restrictedUntil(penalties),
        average,
        ratings.size(),
        penalties.stream().map(this::view).toList());
  }

  /** 최근 90일 유효 패널티가 2회 이상이면 마지막 패널티일 + 30일까지 제한합니다. 제한이 끝났거나 없으면 null입니다. */
  static LocalDateTime restrictedUntil(List<MemberPenalty> penalties) {
    LocalDateTime now = LocalDateTime.now();
    List<MemberPenalty> recent =
        penalties.stream()
            .filter(MemberPenalty::isActive)
            .filter(p -> p.getCreatedAt().isAfter(now.minusDays(WINDOW_DAYS)))
            .toList();
    if (recent.size() < PENALTY_LIMIT) return null;
    LocalDateTime latest =
        recent.stream().map(MemberPenalty::getCreatedAt).max(LocalDateTime::compareTo).orElse(now);
    LocalDateTime until = latest.plusDays(RESTRICT_DAYS);
    return now.isBefore(until) ? until : null;
  }

  private ParticipationDtos.Penalty view(MemberPenalty penalty) {
    String nickname =
        memberRepository.findById(penalty.getMemberId()).map(Member::getNickname).orElse("회원");
    String title =
        applicationRepository
            .findById(penalty.getApplicationId())
            .map(application -> application.getCampaign().getTitle())
            .orElse(null);
    return ParticipationDtos.Penalty.from(penalty, nickname, title);
  }

  private void requireManager(Long viewerId, CampaignApplication application) {
    if (memberService.getMember(viewerId).isAdmin()) return;
    campaignOwnership.requireOwner(viewerId, application.getCampaign());
  }

  private void requireAdmin(Long memberId) {
    if (!memberService.getMember(memberId).isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영자만 할 수 있습니다.");
    }
  }

  private CampaignApplication getApplication(Long applicationId) {
    return applicationRepository
        .findById(applicationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
  }

  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
