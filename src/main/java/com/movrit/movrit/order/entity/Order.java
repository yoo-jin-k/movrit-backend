package com.movrit.movrit.order.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_orders_order_number", columnNames = "order_number"),
        @UniqueConstraint(name = "uk_orders_request_key", columnNames = "request_key")
})
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

    public void update(OrderStatus status, String receiverName, String receiverPhone, String postalCode,
                       String address1, String address2, LocalDateTime paidAt, LocalDateTime cancelledAt) {
        this.status = status;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.postalCode = postalCode;
        this.address1 = address1;
        this.address2 = address2;
        this.paidAt = paidAt;
        this.cancelledAt = cancelledAt;
    }

    public void markPaid() {
        if (status != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("결제대기 주문만 결제완료 처리할 수 있어.");
        status = OrderStatus.PAID;
        paidAt = LocalDateTime.now();
    }

    public void markPreparing() {
        if (status != OrderStatus.PAID) throw new IllegalStateException("결제완료 주문만 배송 준비할 수 있어.");
        status = OrderStatus.PREPARING;
    }

    public void markShipped() {
        if (status != OrderStatus.PREPARING) throw new IllegalStateException("배송 준비 주문만 배송중 처리할 수 있어.");
        status = OrderStatus.SHIPPED;
    }

    public void markDelivered() {
        if (status != OrderStatus.SHIPPED) throw new IllegalStateException("배송중 주문만 배송완료 처리할 수 있어.");
        status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("배송이 시작된 주문은 일반 취소할 수 없어.");
        }
        if (status == OrderStatus.CANCELLED) return;
        status = OrderStatus.CANCELLED;
        cancelledAt = LocalDateTime.now();
    }

    public void markRefunded() {
        status = OrderStatus.REFUNDED;
    }
}
