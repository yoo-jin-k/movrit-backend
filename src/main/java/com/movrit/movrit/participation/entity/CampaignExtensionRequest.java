package com.movrit.movrit.participation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기간 연장 요청입니다. 참여자가 7일 또는 14일 연장을 요청하고 캠페인 업체(또는 운영자)가 승인·반려합니다.
 *
 * <p>신청당 한 번만 요청할 수 있고(반려되어도 다시 요청하지 않습니다), 승인되면 그 신청의 콘텐츠 제출 마감만 늦춥니다.
 */
@Entity
@Table(name = "campaign_extension_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignExtensionRequest {
  public static final String REQUESTED = "REQUESTED";
  public static final String APPROVED = "APPROVED";
  public static final String REJECTED = "REJECTED";

  /** 요청할 수 있는 연장 일수입니다. */
  public static final Set<Integer> ALLOWED_DAYS = Set.of(7, 14);

  @Id
  @Column(name = "application_id")
  private Long applicationId;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(nullable = false)
  private int days;

  @Column(nullable = false, length = 500)
  private String reason;

  /** 요청할 때의 제출 마감입니다. 승인하면 이 시각에 days일을 더합니다. */
  @Column(name = "previous_deadline", nullable = false)
  private LocalDateTime previousDeadline;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "decision_note", length = 500)
  private String decisionNote;

  @Column(name = "decided_by_member_id")
  private Long decidedByMemberId;

  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public CampaignExtensionRequest(
      Long applicationId, Long memberId, int days, String reason, LocalDateTime previousDeadline) {
    if (!ALLOWED_DAYS.contains(days)) {
      throw new IllegalArgumentException("연장은 7일 또는 14일만 요청할 수 있습니다.");
    }
    this.applicationId = applicationId;
    this.memberId = memberId;
    this.days = days;
    this.reason = reason;
    this.previousDeadline = previousDeadline;
    this.status = REQUESTED;
    this.createdAt = LocalDateTime.now();
  }

  public boolean isOpen() {
    return REQUESTED.equals(status);
  }

  /** 승인했을 때의 새 마감입니다. */
  public LocalDateTime extendedDeadline() {
    return previousDeadline.plusDays(days);
  }

  /** 업체·운영자가 승인하거나 반려합니다. 확인 중인 요청만 한 번 처리할 수 있습니다. */
  public void decide(boolean approve, Long deciderMemberId, String note) {
    if (!isOpen()) throw new IllegalStateException("이미 처리한 기간 연장 요청입니다.");
    this.status = approve ? APPROVED : REJECTED;
    this.decidedByMemberId = deciderMemberId;
    this.decisionNote = note;
    this.decidedAt = LocalDateTime.now();
  }
}
