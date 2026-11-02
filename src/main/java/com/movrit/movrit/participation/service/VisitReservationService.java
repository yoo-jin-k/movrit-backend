package com.movrit.movrit.participation.service;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.participation.dto.ParticipationDtos;
import com.movrit.movrit.participation.entity.VisitReservation;
import com.movrit.movrit.participation.repository.VisitReservationRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 방문형 캠페인의 방문 예약을 처리합니다.
 *
 * <p>흐름: 선정되어 참여 중인 참여자가 방문 희망 일시·인원을 요청 → 업체가 확정하거나 사유와 함께 반려 → 반려되면 다른 일시로 다시 요청. 확정 뒤에도 방문 완료 확인
 * 전이면 일시를 바꿔 다시 요청할 수 있습니다. 체험 기간이 정해져 있으면 그 기간 안의 일시만 받습니다.
 */
@Service
@RequiredArgsConstructor
public class VisitReservationService {
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

  private final CampaignApplicationRepository applicationRepository;
  private final VisitReservationRepository reservationRepository;
  private final CampaignOwnershipService campaignOwnership;
  private final MemberService memberService;
  private final NotificationService notificationService;

  /**
   * 참여자가 방문 일시를 요청합니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 방문형 진행 중이 아니거나 이미 방문 확인을 했으면 409, 일시가 맞지 않으면
   *     400
   */
  @Transactional
  public ParticipationDtos.Reservation request(
      Long memberId, Long applicationId, ParticipationDtos.ReservationRequest request) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    if (application.getFulfillmentType() != FulfillmentType.VISIT
        || application.getStatus() != CampaignApplicationStatus.ACTIVE) {
      throw conflict("선정되어 참여 중인 방문형 캠페인에서만 방문 예약을 할 수 있습니다.");
    }
    if (application.getExperienceConfirmedAt() != null) {
      throw conflict("방문 완료를 확인한 뒤에는 예약을 바꿀 수 없습니다.");
    }
    LocalDateTime visitAt = request.visitAt().withSecond(0).withNano(0);
    if (!visitAt.isAfter(LocalDateTime.now())) throw badRequest("지금 이후의 방문 일시를 골라 주세요.");
    Campaign campaign = application.getCampaign();
    if (campaign.getExperienceStartAt() != null
        && visitAt.toLocalDate().isBefore(campaign.getExperienceStartAt().toLocalDate())) {
      throw badRequest("체험 기간이 시작된 뒤의 일시를 골라 주세요.");
    }
    if (campaign.getExperienceEndAt() != null && visitAt.isAfter(campaign.getExperienceEndAt())) {
      throw badRequest("체험 기간 안의 일시를 골라 주세요.");
    }
    VisitReservation reservation =
        reservationRepository
            .findById(applicationId)
            .orElseGet(() -> new VisitReservation(applicationId));
    String note =
        request.note() == null || request.note().isBlank() ? null : request.note().strip();
    reservation.request(visitAt, request.partySize(), note);
    reservationRepository.save(reservation);
    notificationService.send(
        campaign.getBrand().getSeller().getMember().getId(),
        "CAMPAIGN_VISIT",
        "방문 예약 요청이 도착했습니다.",
        application.getCreator().getNickname()
            + "님 · "
            + visitAt.format(TIME)
            + " · "
            + request.partySize()
            + "명",
        "CAMPAIGN_APPLICATION",
        applicationId);
    return ParticipationDtos.Reservation.from(reservation);
  }

  /** 예약을 조회합니다. 참여자 본인·캠페인 업체·운영자만 볼 수 있고, 아직 요청 전이면 비어 있습니다. */
  @Transactional(readOnly = true)
  public Optional<ParticipationDtos.Reservation> read(Long memberId, Long applicationId) {
    CampaignApplication application = getApplication(applicationId);
    boolean mine = application.getCreator().getMember().getId().equals(memberId);
    if (!mine && !memberService.getMember(memberId).isAdmin()) {
      campaignOwnership.requireOwner(memberId, application.getCampaign());
    }
    return reservationRepository.findById(applicationId).map(ParticipationDtos.Reservation::from);
  }

  /** 업체가 예약을 확정합니다. */
  @Transactional
  public ParticipationDtos.Reservation confirm(Long sellerMemberId, Long applicationId) {
    CampaignApplication application = getApplication(applicationId);
    campaignOwnership.requireOwner(sellerMemberId, application.getCampaign());
    VisitReservation reservation = getReservation(applicationId);
    try {
      reservation.confirm();
    } catch (IllegalStateException exception) {
      throw conflict(exception.getMessage());
    }
    notificationService.send(
        application.getCreator().getMember().getId(),
        "CAMPAIGN_VISIT",
        "방문 예약이 확정되었습니다.",
        application.getCampaign().getTitle() + " · " + reservation.getVisitAt().format(TIME),
        "CAMPAIGN_APPLICATION",
        applicationId);
    return ParticipationDtos.Reservation.from(reservation);
  }

  /** 업체가 사유와 함께 예약을 반려합니다. 참여자는 다른 일시로 다시 요청할 수 있습니다. */
  @Transactional
  public ParticipationDtos.Reservation reject(
      Long sellerMemberId, Long applicationId, ParticipationDtos.ReasonRequest request) {
    CampaignApplication application = getApplication(applicationId);
    campaignOwnership.requireOwner(sellerMemberId, application.getCampaign());
    VisitReservation reservation = getReservation(applicationId);
    try {
      reservation.reject(request.reason().trim());
    } catch (IllegalStateException exception) {
      throw conflict(exception.getMessage());
    }
    notificationService.send(
        application.getCreator().getMember().getId(),
        "CAMPAIGN_VISIT",
        "방문 예약을 다시 정해 주세요.",
        application.getCampaign().getTitle() + " · " + request.reason().trim(),
        "CAMPAIGN_APPLICATION",
        applicationId);
    return ParticipationDtos.Reservation.from(reservation);
  }

  private VisitReservation getReservation(Long applicationId) {
    return reservationRepository
        .findById(applicationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "방문 예약이 없습니다."));
  }

  private CampaignApplication getApplication(Long applicationId) {
    return applicationRepository
        .findById(applicationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
  }

  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  private static ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
