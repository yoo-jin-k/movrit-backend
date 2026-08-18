package com.movrit.movrit.shipping.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Shipping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "courier_code")
    private String courierCode;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShippingStatus status;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    public void update(String courierCode, String trackingNumber, ShippingStatus status,
                       LocalDateTime shippedAt, LocalDateTime deliveredAt) {
        this.courierCode = courierCode;
        this.trackingNumber = trackingNumber;
        this.status = status;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
    }

    public void prepare() {
        if (status != ShippingStatus.READY) throw new IllegalStateException("배송 대기 상태에서만 준비를 시작할 수 있어.");
        this.status = ShippingStatus.PREPARING;
    }

    public void ship(String courierCode, String trackingNumber) {
        if (status != ShippingStatus.PREPARING) throw new IllegalStateException("배송 준비 상태에서만 발송할 수 있어.");
        if (courierCode == null || courierCode.isBlank() || trackingNumber == null || trackingNumber.isBlank())
            throw new IllegalArgumentException("택배사와 운송장 번호를 입력해줘.");
        this.courierCode = courierCode.trim();
        this.trackingNumber = trackingNumber.trim();
        this.status = ShippingStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }

    public void deliver() {
        if (status != ShippingStatus.SHIPPED) throw new IllegalStateException("발송된 배송만 완료할 수 있어.");
        this.status = ShippingStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }
}
