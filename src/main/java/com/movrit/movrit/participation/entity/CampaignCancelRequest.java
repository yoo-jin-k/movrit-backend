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
 * 협의 취소 요청입니다. 업체와 취소를 합의했거나 업체 사정으로 진행이 어려울 때, 참여자가 사유와 증빙(대화 캡처 등)을 올려 요청합니다.
 *
 * <p>운영자가 승인하면 취소 횟수 없이 참여를 취소하고, 반려하면 참여는 그대로 이어집니다. 신청 한 건에 확인 중인 요청은 하나만 둡니다.
 */
@Entity
@Table(name = "campaign_cancel_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignCancelRequest {
  public static final String REQUESTED = "REQUESTED";
  public static final String APPROVED = "APPROVED";
  public static final String REJECTED = "REJECTED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(nullable = false, length = 500)
  private String reason;

  /** 비공개 폴더에 저장한 증빙 이미지 키입니다. 공개 주소 없이 API로만 내려갑니다. */
  @Column(name = "evidence_key", nullable = false, length = 100)
  private String evidenceKey;

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

  public CampaignCancelRequest(
      Long applicationId, Long memberId, String reason, String evidenceKey) {
    this.applicationId = applicationId;
    this.memberId = memberId;
    this.reason = reason;
    this.evidenceKey = evidenceKey;
    this.status = REQUESTED;
    this.createdAt = LocalDateTime.now();
  }

  public boolean isOpen() {
    return REQUESTED.equals(status);
  }

  /** 운영자가 승인하거나 반려합니다. 확인 중인 요청만 한 번 처리할 수 있습니다. */
  public void decide(boolean approve, Long adminMemberId, String note) {
    if (!isOpen()) throw new IllegalStateException("이미 처리한 협의 취소 요청입니다.");
    this.status = approve ? APPROVED : REJECTED;
    this.decidedByMemberId = adminMemberId;
    this.decisionNote = note;
    this.decidedAt = LocalDateTime.now();
  }
}
