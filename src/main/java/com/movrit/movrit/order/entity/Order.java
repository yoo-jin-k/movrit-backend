package com.movrit.movrit.order.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.preorder.entity.Preorder;
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
 * 프리오더 주문 한 건입니다.
 *
 * <p>결제 대기(PENDING_PAYMENT) → 결제 완료(PAID) → 배송 준비(PREPARING) → 발송(SHIPPED) → 배송 완료(DELIVERED) 순서로
 * 진행합니다. 결제 전에는 취소(CANCELLED), 결제 후 배송 전에는 환불(REFUNDED)할 수 있습니다. 주문 당시의 받는 사람·금액은 주문에 그대로 보관합니다.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_number", nullable = false)
  private String orderNumber;

  @Column(name = "request_key", nullable = false)
  private String requestKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "preorder_id", nullable = false)
  private Preorder preorder;

  @Column(name = "attribution_id")
  private Long attributionId;

  @Column(name = "acquisition_source", nullable = false)
  private String acquisitionSource;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(name = "total_product_amount", nullable = false)
  private BigDecimal totalProductAmount;

  @Column(name = "discount_amount", nullable = false)
  private BigDecimal discountAmount;

  @Column(name = "shipping_amount", nullable = false)
  private BigDecimal shippingAmount;

  @Column(name = "total_payment_amount", nullable = false)
  private BigDecimal totalPaymentAmount;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, length = 3, columnDefinition = "char(3)")
  private String currency;

  @Column(name = "receiver_name", nullable = false)
  private String receiverName;

  @Column(name = "receiver_phone", nullable = false)
  private String receiverPhone;

  @Column(name = "postal_code")
  private String postalCode;

  @Column(name = "address1", nullable = false)
  private String address1;

  @Column(name = "address2")
  private String address2;

  @Column(name = "paid_at")
  private LocalDateTime paidAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  /** 받는 사람 정보를 바꿉니다. 배송 준비가 시작되기 전(결제 대기·결제 완료)까지만 바꿀 수 있습니다. */
  public void updateReceiver(
      String receiverName,
      String receiverPhone,
      String postalCode,
      String address1,
      String address2) {
    if (status != OrderStatus.PENDING_PAYMENT && status != OrderStatus.PAID) {
      throw new IllegalStateException("배송 준비가 시작된 주문은 배송지를 변경할 수 없습니다.");
    }
    this.receiverName = receiverName;
    this.receiverPhone = receiverPhone;
    this.postalCode = postalCode;
    this.address1 = address1;
    this.address2 = address2;
  }

  /** 결제가 승인되어 결제 완료 상태로 바꾸고 결제 시각을 기록합니다. */
  public void markPaid() {
    requireStatus(OrderStatus.PENDING_PAYMENT);
    this.status = OrderStatus.PAID;
    this.paidAt = LocalDateTime.now();
  }

  /** 업체가 배송 준비를 시작해 배송 준비 상태로 바꿉니다. */
  public void markPreparing() {
    requireStatus(OrderStatus.PAID);
    this.status = OrderStatus.PREPARING;
  }

  /** 택배를 보내 발송 상태로 바꿉니다. */
  public void markShipped() {
    requireStatus(OrderStatus.PREPARING);
    this.status = OrderStatus.SHIPPED;
  }

  /** 배송이 끝나 배송 완료 상태로 바꿉니다. */
  public void markDelivered() {
    requireStatus(OrderStatus.SHIPPED);
    this.status = OrderStatus.DELIVERED;
  }

  /** 결제 전 주문을 취소합니다. 결제가 끝난 주문은 환불로 처리해야 합니다. */
  public void cancel() {
    if (status != OrderStatus.PENDING_PAYMENT)
      throw new IllegalStateException("결제 대기 주문만 바로 취소할 수 있습니다.");
    this.status = OrderStatus.CANCELLED;
    this.cancelledAt = LocalDateTime.now();
  }

  /** 결제 완료 또는 배송 준비 중인 주문을 환불 상태로 바꿉니다. 발송 후에는 환불할 수 없습니다. */
  public void refund() {
    if (status != OrderStatus.PAID && status != OrderStatus.PREPARING) {
      throw new IllegalStateException("결제 완료 또는 배송 준비 주문만 환불할 수 있습니다.");
    }
    this.status = OrderStatus.REFUNDED;
    this.cancelledAt = LocalDateTime.now();
  }

  private void requireStatus(OrderStatus expected) {
    if (status != expected) throw new IllegalStateException("현재 주문 상태에서는 처리할 수 없습니다.");
  }
}
