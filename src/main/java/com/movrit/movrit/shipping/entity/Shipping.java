package com.movrit.movrit.shipping.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 한 건의 배송 정보(택배사·운송장 번호·발송/완료 시각)입니다.
 *
 * <p>배송 대기(READY) → 준비 중(PREPARING) → 발송(SHIPPED) → 배송 완료(DELIVERED) 순서로 진행합니다.
 */
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

  /** 관리용으로 배송 정보를 한 번에 덮어씁니다. 일반 흐름에서는 prepare·ship·deliver를 사용합니다. */
  public void update(
      String courierCode,
      String trackingNumber,
      ShippingStatus status,
      LocalDateTime shippedAt,
      LocalDateTime deliveredAt) {
    this.courierCode = courierCode;
    this.trackingNumber = trackingNumber;
    this.status = status;
    this.shippedAt = shippedAt;
    this.deliveredAt = deliveredAt;
  }

  /** 배송 대기 상태에서 준비를 시작합니다. */
  public void prepare() {
    if (status != ShippingStatus.READY)
      throw new IllegalStateException("배송 대기 상태에서만 준비를 시작할 수 있습니다.");
    this.status = ShippingStatus.PREPARING;
  }

  /** 택배사와 운송장 번호를 기록하고 발송 상태로 바꿉니다. 둘 중 하나라도 비어 있으면 거절합니다. */
  public void ship(String courierCode, String trackingNumber) {
    if (status != ShippingStatus.PREPARING)
      throw new IllegalStateException("배송 준비 상태에서만 발송할 수 있습니다.");
    if (courierCode == null
        || courierCode.isBlank()
        || trackingNumber == null
        || trackingNumber.isBlank()) throw new IllegalArgumentException("택배사와 운송장 번호를 입력해 주세요.");
    this.courierCode = courierCode.trim();
    this.trackingNumber = trackingNumber.trim();
    this.status = ShippingStatus.SHIPPED;
    this.shippedAt = LocalDateTime.now();
  }

  /** 발송된 배송을 완료 처리하고 완료 시각을 기록합니다. */
  public void deliver() {
    if (status != ShippingStatus.SHIPPED) throw new IllegalStateException("발송된 배송만 완료할 수 있습니다.");
    this.status = ShippingStatus.DELIVERED;
    this.deliveredAt = LocalDateTime.now();
  }
}
