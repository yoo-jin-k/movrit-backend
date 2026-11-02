package com.movrit.movrit.participation.dto;

import com.movrit.movrit.participation.entity.MemberPenalty;
import com.movrit.movrit.participation.entity.VisitReservation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** 방문 예약·패널티·활동 등급·참여자 평가 API의 요청·응답 형식입니다. */
public final class ParticipationDtos {
  private ParticipationDtos() {}

  /** 방문 예약 요청입니다. visitAt은 한국 시간입니다. */
  public record ReservationRequest(
      @NotNull LocalDateTime visitAt,
      @NotNull @Min(1) @Max(10) Integer partySize,
      @Size(max = 500) String note) {}

  /** 반려·취소처럼 사유가 꼭 필요한 요청입니다. */
  public record ReasonRequest(@NotBlank @Size(max = 500) String reason) {}

  public record Reservation(
      Long applicationId,
      LocalDateTime visitAt,
      int partySize,
      String requestNote,
      String status,
      String rejectReason,
      LocalDateTime requestedAt,
      LocalDateTime decidedAt) {
    public static Reservation from(VisitReservation value) {
      return new Reservation(
          value.getApplicationId(),
          value.getVisitAt(),
          value.getPartySize(),
          value.getRequestNote(),
          value.getStatus(),
          value.getRejectReason(),
          value.getRequestedAt(),
          value.getDecidedAt());
    }
  }

  /**
   * 패널티 기록 요청입니다.
   *
   * @param type NO_SUBMIT(미제출) 또는 NO_SHOW(노쇼)
   */
  public record PenaltyRequest(@NotBlank String type, @NotBlank @Size(max = 500) String reason) {}

  public record Penalty(
      Long id,
      Long memberId,
      String memberNickname,
      Long applicationId,
      String campaignTitle,
      String type,
      String reason,
      LocalDateTime createdAt,
      boolean active,
      LocalDateTime revokedAt,
      String revokeReason) {
    public static Penalty from(MemberPenalty value, String nickname, String campaignTitle) {
      return new Penalty(
          value.getId(),
          value.getMemberId(),
          nickname,
          value.getApplicationId(),
          campaignTitle,
          value.getPenaltyType(),
          value.getReason(),
          value.getCreatedAt(),
          value.isActive(),
          value.getRevokedAt(),
          value.getRevokeReason());
    }
  }

  /** 완료 참여자 평가 요청입니다(1~5점, 메모는 남긴 브랜드만 봅니다). */
  public record RatingRequest(
      @NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) String memo) {}

  /**
   * 활동 등급과 패널티 요약입니다.
   *
   * @param grade SPROUT·BRONZE·SILVER·GOLD·DIAMOND
   * @param remainingToNext 다음 등급까지 남은 완료 횟수(최고 등급이면 0)
   * @param restrictedUntil 신청 제한이 끝나는 시각(제한이 없으면 null)
   * @param ratingAverage 업체 평가 평균(평가가 없으면 null). 참여자 본인 화면에는 내려주지 않습니다.
   */
  public record Activity(
      String grade,
      String gradeLabel,
      String nextGradeLabel,
      int remainingToNext,
      long completedCount,
      long activePenaltyCount,
      LocalDateTime restrictedUntil,
      Double ratingAverage,
      long ratingCount,
      List<Penalty> penalties) {}

  /** 업체 참여자 상세에서 보는 활동 정보와 이 신청에 남긴 내 평가입니다. */
  public record ParticipantActivity(Activity activity, Integer myRating, String myMemo) {}
}
