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

  public void markPaid() {
    requireStatus(OrderStatus.PENDING_PAYMENT);
    this.status = OrderStatus.PAID;
    this.paidAt = LocalDateTime.now();
  }

  public void markPreparing() {
    requireStatus(OrderStatus.PAID);
    this.status = OrderStatus.PREPARING;
  }

  public void markShipped() {
    requireStatus(OrderStatus.PREPARING);
    this.status = OrderStatus.SHIPPED;
  }

  public void markDelivered() {
    requireStatus(OrderStatus.SHIPPED);
    this.status = OrderStatus.DELIVERED;
  }

  public void cancel() {
    if (status != OrderStatus.PENDING_PAYMENT)
      throw new IllegalStateException("결제 대기 주문만 바로 취소할 수 있습니다.");
    this.status = OrderStatus.CANCELLED;
    this.cancelledAt = LocalDateTime.now();
  }

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
