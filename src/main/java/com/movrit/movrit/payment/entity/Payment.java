package com.movrit.movrit.payment.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 주문 한 건의 결제입니다.
 *
 * <p>결제 대기(READY)에서 승인(PAID) 또는 실패(FAILED)로 바뀌고, 승인된 결제는 환불(REFUNDED)할 수 있습니다. 실제 결제사 연동 없이 승인 키를
 * 받아 기록하는 구조입니다.
 */
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

  @Column(name = "failure_message", length = 500)
  private String failureMessage;

  /** 관리용으로 결제 정보를 한 번에 덮어씁니다. 일반 흐름에서는 approve·fail·refund를 사용합니다. */
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

  /** 결제 대기 상태의 결제를 승인 완료로 바꾸고, 요청 금액을 승인 금액으로 기록합니다. */
  public void approve(String paymentKey, String pgProvider, String method) {
    if (status != PaymentStatus.READY) throw new IllegalStateException("결제 대기 상태에서만 승인할 수 있습니다.");
    this.paymentKey = paymentKey;
    this.pgProvider = pgProvider;
    this.method = method;
    this.status = PaymentStatus.PAID;
    this.approvedAmount = requestedAmount;
    this.approvedAt = LocalDateTime.now();
  }

  /** 결제 대기 상태의 결제를 실패로 바꾸고 실패 코드·사유를 기록합니다. */
  public void fail(String code, String message) {
    if (status != PaymentStatus.READY)
      throw new IllegalStateException("결제 대기 상태에서만 실패 처리할 수 있습니다.");
    this.status = PaymentStatus.FAILED;
    this.failedAt = LocalDateTime.now();
    this.failureCode = code;
    this.failureMessage = message;
  }

  /** 승인된 결제를 환불 상태로 바꿉니다. */
  public void refund() {
    if (status != PaymentStatus.PAID) throw new IllegalStateException("승인된 결제만 환불할 수 있습니다.");
    this.status = PaymentStatus.REFUNDED;
  }
}
