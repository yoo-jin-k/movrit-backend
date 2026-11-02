package com.movrit.movrit.participation.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.participation.dto.ParticipationDtos;
import com.movrit.movrit.participation.service.ActivityService;
import com.movrit.movrit.participation.service.DeadlineReminderService;
import com.movrit.movrit.participation.service.VisitReservationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 참여 운영 API입니다.
 *
 * <ul>
 *   <li>GET /api/members/me/activity: 내 활동 등급·패널티·신청 제한
 *   <li>GET·POST /api/campaigns/applications/{id}/visit-reservation: 방문 예약 조회(없으면 204)·요청(참여자)
 *   <li>PATCH …/visit-reservation/confirm, …/visit-reservation/reject: 업체 확정·반려(사유 필수)
 *   <li>POST /api/campaigns/applications/{id}/penalty: 업체 미제출·노쇼 기록(참여 취소됨)
 *   <li>GET /api/campaigns/applications/{id}/participant-activity: 업체용 참여자 등급·평가 평균·내 평가
 *   <li>PUT /api/campaigns/applications/{id}/rating: 업체가 완료 참여자 평가(1~5점·메모)
 *   <li>GET /api/admin/penalties, PATCH /api/admin/penalties/{id}/revoke: 운영자 패널티 목록·취소
 *   <li>POST /api/admin/deadline-reminders/run: 운영자가 마감 알림 확인을 바로 실행(점검용)
 * </ul>
 */
@Tag(name = "공통 · 참여 운영(방문 예약·평가·패널티)")
@RestController
@RequiredArgsConstructor
public class ParticipationController {
  private final ActivityService activityService;
  private final VisitReservationService reservationService;
  private final DeadlineReminderService reminderService;
  private final MemberService memberService;

  /** 내 활동 등급·패널티·신청 제한 기간을 조회합니다. */
  @GetMapping("/api/members/me/activity")
  public ParticipationDtos.Activity myActivity(@AuthenticationPrincipal Jwt jwt) {
    return activityService.myActivity(JwtMemberId.from(jwt));
  }

  /** 방문 캠페인 신청의 방문 예약을 조회합니다. 예약이 없으면 204를 돌려줍니다. */
  @GetMapping("/api/campaigns/applications/{applicationId}/visit-reservation")
  public ResponseEntity<ParticipationDtos.Reservation> reservation(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return reservationService
        .read(JwtMemberId.from(jwt), applicationId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /** 선정된 참여자가 방문 희망 날짜·시간을 요청합니다. */
  @PostMapping("/api/campaigns/applications/{applicationId}/visit-reservation")
  public ParticipationDtos.Reservation requestReservation(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody ParticipationDtos.ReservationRequest request) {
    return reservationService.request(JwtMemberId.from(jwt), applicationId, request);
  }

  /** 캠페인 업체가 방문 예약을 확정합니다. */
  @PatchMapping("/api/campaigns/applications/{applicationId}/visit-reservation/confirm")
  public ParticipationDtos.Reservation confirmReservation(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return reservationService.confirm(JwtMemberId.from(jwt), applicationId);
  }

  /** 캠페인 업체가 방문 예약 요청을 반려하고 사유를 남깁니다. */
  @PatchMapping("/api/campaigns/applications/{applicationId}/visit-reservation/reject")
  public ParticipationDtos.Reservation rejectReservation(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody ParticipationDtos.ReasonRequest request) {
    return reservationService.reject(JwtMemberId.from(jwt), applicationId, request);
  }

  /** 캠페인 업체가 콘텐츠 미제출·노쇼를 기록합니다. 해당 참여는 취소됩니다. */
  @PostMapping("/api/campaigns/applications/{applicationId}/penalty")
  public ParticipationDtos.Penalty penalty(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody ParticipationDtos.PenaltyRequest request) {
    return activityService.recordPenalty(JwtMemberId.from(jwt), applicationId, request);
  }

  /** 캠페인 업체가 참여자의 활동 등급·평가 평균과 내가 남긴 평가를 조회합니다. */
  @GetMapping("/api/campaigns/applications/{applicationId}/participant-activity")
  public ParticipationDtos.ParticipantActivity participantActivity(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return activityService.participantActivity(JwtMemberId.from(jwt), applicationId);
  }

  /** 캠페인 업체가 참여를 마친 참여자를 1~5점과 메모로 평가합니다. */
  @PutMapping("/api/campaigns/applications/{applicationId}/rating")
  public ParticipationDtos.ParticipantActivity rate(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody ParticipationDtos.RatingRequest request) {
    return activityService.rate(JwtMemberId.from(jwt), applicationId, request);
  }

  /** 운영자가 전체 패널티 목록을 조회합니다. */
  @GetMapping("/api/admin/penalties")
  public List<ParticipationDtos.Penalty> penalties(@AuthenticationPrincipal Jwt jwt) {
    return activityService.adminPenalties(JwtMemberId.from(jwt));
  }

  /** 운영자가 패널티를 취소합니다. */
  @PatchMapping("/api/admin/penalties/{penaltyId}/revoke")
  public ParticipationDtos.Penalty revoke(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long penaltyId,
      @Valid @RequestBody ParticipationDtos.ReasonRequest request) {
    return activityService.revoke(JwtMemberId.from(jwt), penaltyId, request);
  }

  /** 운영자가 마감 알림 확인을 지금 바로 한 번 실행합니다(점검용). */
  @PostMapping("/api/admin/deadline-reminders/run")
  public Map<String, Integer> runReminders(@AuthenticationPrincipal Jwt jwt) {
    if (!memberService.getMember(JwtMemberId.from(jwt)).isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영자만 할 수 있습니다.");
    }
    return Map.of("sent", reminderService.run());
  }
}
