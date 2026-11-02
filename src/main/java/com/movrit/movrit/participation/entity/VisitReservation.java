package com.movrit.movrit.participation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방문형 캠페인의 방문 예약입니다(신청당 1건).
 *
 * <p>상태: REQUESTED(참여자 요청) → CONFIRMED(업체 확정) 또는 REJECTED(업체 반려, 사유 포함). 참여자가 다시 요청하면 새 일시로
 * REQUESTED가 됩니다(확정된 예약을 바꾸는 경우도 같습니다).
 */
@Entity
@Table(name = "campaign_visit_reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitReservation {
  public static final String REQUESTED = "REQUESTED";
  public static final String CONFIRMED = "CONFIRMED";
  public static final String REJECTED = "REJECTED";

  @Id
  @Column(name = "application_id")
  private Long applicationId;

  @Column(name = "visit_at", nullable = false)
  private LocalDateTime visitAt;

  @Column(name = "party_size", nullable = false)
  private Integer partySize;

  @Column(name = "request_note", length = 500)
  private String requestNote;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "reject_reason", length = 500)
  private String rejectReason;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  public VisitReservation(Long applicationId) {
    this.applicationId = applicationId;
  }

  /** 방문 희망 일시를 (다시) 요청합니다. 이전 결정은 지웁니다. */
  public void request(LocalDateTime visitAt, int partySize, String note) {
    this.visitAt = visitAt;
    this.partySize = partySize;
    this.requestNote = note;
    this.status = REQUESTED;
    this.rejectReason = null;
    this.requestedAt = LocalDateTime.now();
    this.decidedAt = null;
  }

  /** 업체가 확정합니다. 요청 상태에서만 가능합니다. */
  public void confirm() {
    requireRequested();
    this.status = CONFIRMED;
    this.decidedAt = LocalDateTime.now();
  }

  /** 업체가 반려합니다. 요청 상태에서만 가능하며 사유가 필요합니다. */
  public void reject(String reason) {
    requireRequested();
    this.status = REJECTED;
    this.rejectReason = reason;
    this.decidedAt = LocalDateTime.now();
  }

  public boolean isConfirmed() {
    return CONFIRMED.equals(status);
  }

  private void requireRequested() {
    if (!REQUESTED.equals(status)) throw new IllegalStateException("확인 대기 중인 예약만 처리할 수 있습니다.");
  }
}
