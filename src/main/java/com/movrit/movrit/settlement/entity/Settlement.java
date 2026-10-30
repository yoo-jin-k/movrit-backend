package com.movrit.movrit.settlement.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "settlement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Settlement extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "preorder_id", nullable = false)
  private Preorder preorder;

  @Column(name = "settlement_number", nullable = false)
  private String settlementNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SettlementStatus status;

  @Column(name = "gross_amount", nullable = false)
  private BigDecimal grossAmount;

  @Column(name = "refund_amount", nullable = false)
  private BigDecimal refundAmount;

  @Column(name = "platform_fee_amount", nullable = false)
  private BigDecimal platformFeeAmount;

  @Column(name = "net_amount", nullable = false)
  private BigDecimal netAmount;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, length = 3, columnDefinition = "char(3)")
  private String currency;

  @Column(name = "calculated_at")
  private LocalDateTime calculatedAt;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @Column(name = "paid_at")
  private LocalDateTime paidAt;

  public void update(
      SettlementStatus status,
      BigDecimal grossAmount,
      BigDecimal refundAmount,
      BigDecimal platformFeeAmount,
      BigDecimal netAmount,
      LocalDateTime calculatedAt,
      LocalDateTime confirmedAt,
      LocalDateTime paidAt) {
    this.status = status;
    this.grossAmount = grossAmount;
    this.refundAmount = refundAmount;
    this.platformFeeAmount = platformFeeAmount;
    this.netAmount = netAmount;
    this.calculatedAt = calculatedAt;
    this.confirmedAt = confirmedAt;
    this.paidAt = paidAt;
  }

  public void confirm() {
    if (status != SettlementStatus.PENDING)
      throw new IllegalStateException("정산 대기 상태에서만 확정할 수 있습니다.");
    this.status = SettlementStatus.CONFIRMED;
    this.confirmedAt = LocalDateTime.now();
  }

  public void pay() {
    if (status != SettlementStatus.CONFIRMED)
      throw new IllegalStateException("확정된 정산만 지급 완료할 수 있습니다.");
    this.status = SettlementStatus.PAID;
    this.paidAt = LocalDateTime.now();
  }

  public void cancel() {
    if (status == SettlementStatus.PAID) throw new IllegalStateException("지급 완료된 정산은 취소할 수 없습니다.");
    this.status = SettlementStatus.CANCELLED;
  }
}
