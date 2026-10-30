package com.movrit.movrit.payment.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.order.entity.Order;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "payment_key")
  private String paymentKey;

  @Column(name = "pg_provider")
  private String pgProvider;

  private String method;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Column(name = "requested_amount", nullable = false)
  private BigDecimal requestedAmount;

  @Column(name = "approved_amount")
  private BigDecimal approvedAmount;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, length = 3, columnDefinition = "char(3)")
  private String currency;

  @Column(name = "idempotency_key")
  private String idempotencyKey;

  @Column(name = "requested_at")
  private LocalDateTime requestedAt;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "failed_at")
  private LocalDateTime failedAt;

  @Column(name = "failure_code")
  private String failureCode;

  @Column(name = "failure_message")
  private String failureMessage;

  public void update(
      String paymentKey,
      String pgProvider,
      String method,
      PaymentStatus status,
      BigDecimal approvedAmount,
      LocalDateTime requestedAt,
      LocalDateTime approvedAt,
      LocalDateTime failedAt,
      String failureCode,
      String failureMessage) {
    this.paymentKey = paymentKey;
    this.pgProvider = pgProvider;
    this.method = method;
    this.status = status;
    this.approvedAmount = approvedAmount;
    this.requestedAt = requestedAt;
    this.approvedAt = approvedAt;
    this.failedAt = failedAt;
    this.failureCode = failureCode;
    this.failureMessage = failureMessage;
  }

  public void approve(String paymentKey, String pgProvider, String method) {
    if (status != PaymentStatus.READY) throw new IllegalStateException("결제 대기 상태에서만 승인할 수 있습니다.");
    this.paymentKey = paymentKey;
    this.pgProvider = pgProvider;
    this.method = method;
    this.status = PaymentStatus.PAID;
    this.approvedAmount = requestedAmount;
    this.approvedAt = LocalDateTime.now();
  }

  public void fail(String code, String message) {
    if (status != PaymentStatus.READY)
      throw new IllegalStateException("결제 대기 상태에서만 실패 처리할 수 있습니다.");
    this.status = PaymentStatus.FAILED;
    this.failedAt = LocalDateTime.now();
    this.failureCode = code;
    this.failureMessage = message;
  }

  public void refund() {
    if (status != PaymentStatus.PAID) throw new IllegalStateException("승인된 결제만 환불할 수 있습니다.");
    this.status = PaymentStatus.REFUNDED;
  }
}
