package com.movrit.movrit.order.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(nullable = false, columnDefinition = "char(3)")
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
}
