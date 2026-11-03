package com.movrit.movrit.participation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참여자 패널티입니다(신청당 1건).
 *
 * <ul>
 *   <li>NO_SUBMIT(미제출): 콘텐츠 제출 마감이 지났는데 제출하지 않은 경우
 *   <li>NO_SHOW(노쇼): 확정한 방문 예약 일시가 지났는데 방문하지 않은 경우
 *   <li>CANCEL_LIMIT(취소 누적): 1년 안의 취소 횟수가 5회 쌓인 경우(마지막 취소 신청에 남깁니다)
 * </ul>
 *
 * <p>운영자가 취소(revoke)하면 등급·신청 제한 계산에서 빠집니다. 기록 자체는 지우지 않습니다.
 */
@Entity
@Table(name = "member_penalty")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPenalty {
  public static final String NO_SUBMIT = "NO_SUBMIT";
  public static final String NO_SHOW = "NO_SHOW";

  /** 취소 횟수 5회가 쌓여 생긴 패널티입니다. */
  public static final String CANCEL_LIMIT = "CANCEL_LIMIT";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  @Column(name = "penalty_type", nullable = false, length = 20)
  private String penaltyType;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(name = "created_by_member_id", nullable = false)
  private Long createdByMemberId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Column(name = "revoked_by_member_id")
  private Long revokedByMemberId;

  @Column(name = "revoke_reason", length = 500)
  private String revokeReason;

  public MemberPenalty(
      Long memberId, Long applicationId, String penaltyType, String reason, Long createdBy) {
    this.memberId = memberId;
    this.applicationId = applicationId;
    this.penaltyType = penaltyType;
    this.reason = reason;
    this.createdByMemberId = createdBy;
    this.createdAt = LocalDateTime.now();
  }

  public boolean isActive() {
    return revokedAt == null;
  }

  /** 운영자가 패널티를 취소합니다. 한 번만 할 수 있습니다. */
  public void revoke(Long adminMemberId, String reason) {
    if (!isActive()) throw new IllegalStateException("이미 취소한 패널티입니다.");
    this.revokedAt = LocalDateTime.now();
    this.revokedByMemberId = adminMemberId;
    this.revokeReason = reason;
  }
}
