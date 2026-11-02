package com.movrit.movrit.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.movrit.movrit.participation.entity.MemberPenalty;
import com.movrit.movrit.participation.entity.VisitReservation;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 활동 등급·신청 제한·방문 예약 상태의 기본 규칙을 확인합니다. */
class ParticipationRulesTest {

  @Test
  void gradeFollowsCompletedCount() {
    assertThat(ActivityGrades.of(0).label()).isEqualTo("새싹");
    assertThat(ActivityGrades.of(0).remainingToNext()).isEqualTo(1);
    assertThat(ActivityGrades.of(2).label()).isEqualTo("브론즈");
    assertThat(ActivityGrades.of(2).nextLabel()).isEqualTo("실버");
    assertThat(ActivityGrades.of(5).label()).isEqualTo("실버");
    assertThat(ActivityGrades.of(9).label()).isEqualTo("골드");
    assertThat(ActivityGrades.of(10).label()).isEqualTo("다이아");
    assertThat(ActivityGrades.of(10).nextLabel()).isNull();
  }

  @Test
  void twoRecentPenaltiesRestrictForThirtyDays() {
    MemberPenalty first = penalty(LocalDateTime.now().minusDays(20));
    MemberPenalty second = penalty(LocalDateTime.now().minusDays(5));
    assertThat(ActivityService.restrictedUntil(List.of(first))).isNull();
    LocalDateTime until = ActivityService.restrictedUntil(List.of(first, second));
    assertThat(until).isNotNull();
    assertThat(until.toLocalDate()).isEqualTo(second.getCreatedAt().plusDays(30).toLocalDate());
    // 운영자가 하나를 취소하면 제한이 풀립니다.
    second.revoke(1L, "업체 착오");
    assertThat(ActivityService.restrictedUntil(List.of(first, second))).isNull();
    // 90일보다 오래된 패널티는 세지 않습니다.
    assertThat(
            ActivityService.restrictedUntil(
                List.of(penalty(LocalDateTime.now().minusDays(120)), first)))
        .isNull();
  }

  @Test
  void reservationFlow() {
    VisitReservation reservation = new VisitReservation(1L);
    reservation.request(LocalDateTime.now().plusDays(2), 2, "점심 시간");
    reservation.reject("그 시간은 만석입니다.");
    assertThat(reservation.getStatus()).isEqualTo(VisitReservation.REJECTED);
    assertThatThrownBy(reservation::confirm).isInstanceOf(IllegalStateException.class);
    reservation.request(LocalDateTime.now().plusDays(3), 2, null);
    assertThat(reservation.getRejectReason()).isNull();
    reservation.confirm();
    assertThat(reservation.isConfirmed()).isTrue();
  }

  private static MemberPenalty penalty(LocalDateTime createdAt) {
    MemberPenalty penalty = new MemberPenalty(7L, 1L, MemberPenalty.NO_SUBMIT, "미제출", 2L);
    ReflectionTestUtils.setField(penalty, "createdAt", createdAt);
    return penalty;
  }
}
