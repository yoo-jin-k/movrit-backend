package com.movrit.movrit.purchasereview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참여자의 구매 인증과 환급 기록입니다(신청당 1건).
 *
 * <ul>
 *   <li>구매 인증: 주문번호·결제 금액·주문 내역 캡처. 다시 제출하면 새 값으로 바꿉니다.
 *   <li>환급: 구매평이 승인되면 브랜드가 직접 돈을 보내고 '환급 완료'로 기록합니다. MOVRIT은 송금하지 않습니다.
 * </ul>
 */
@Entity
@Table(name = "campaign_purchase_proof")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignPurchaseProof {
  /** 환급 대기 */
  public static final String PENDING = "PENDING";

  /** 환급 완료 */
  public static final String PAID = "PAID";

  @Id
  @Column(name = "application_id")
  private Long applicationId;

  @Column(name = "order_number", nullable = false, length = 60)
  private String orderNumber;

  @Column(name = "paid_amount", nullable = false)
  private Integer paidAmount;

  @Column(name = "evidence_key", nullable = false, length = 80)
  private String evidenceKey;

  @Column(name = "submitted_at", nullable = false)
  private LocalDateTime submittedAt;

  @Column(name = "payback_status", nullable = false, length = 20)
  private String paybackStatus = PENDING;

  @Column(name = "payback_paid_at")
  private LocalDateTime paybackPaidAt;

  @Column(name = "payback_note", length = 200)
  private String paybackNote;

  public CampaignPurchaseProof(Long applicationId) {
    this.applicationId = applicationId;
  }

  /** 구매 인증을 (다시) 제출합니다. 이전 캡처 저장 키를 돌려줘 서비스가 파일을 지울 수 있게 합니다. */
  public String submit(String orderNumber, int paidAmount, String evidenceKey) {
    if (isPaid()) throw new IllegalStateException("환급이 끝난 구매 인증은 바꿀 수 없습니다.");
    String previous = this.evidenceKey;
    this.orderNumber = orderNumber;
    this.paidAmount = paidAmount;
    this.evidenceKey = evidenceKey;
    this.submittedAt = LocalDateTime.now();
    return previous;
  }

  /** 환급 완료로 기록합니다. 한 번만 할 수 있습니다. */
  public void markPaid(String note) {
    if (isPaid()) throw new IllegalStateException("이미 환급 완료로 기록했습니다.");
    this.paybackStatus = PAID;
    this.paybackPaidAt = LocalDateTime.now();
    this.paybackNote = note;
  }

  public boolean isPaid() {
    return PAID.equals(paybackStatus);
  }
}
