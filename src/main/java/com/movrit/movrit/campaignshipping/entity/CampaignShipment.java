package com.movrit.movrit.campaignshipping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 배송형 참여자의 발송 정보입니다(신청당 1건). 처음 입력한 시각을 발송일로 남기고, 고치면 수정 시각만 바뀝니다. */
@Entity
@Table(name = "campaign_shipment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignShipment {
  @Id
  @Column(name = "application_id")
  private Long applicationId;

  @Column(nullable = false, length = 30)
  private String courier;

  @Column(name = "tracking_number", nullable = false, length = 30)
  private String trackingNumber;

  @Column(name = "shipped_at", nullable = false)
  private LocalDateTime shippedAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public CampaignShipment(Long applicationId, String courier, String trackingNumber) {
    this.applicationId = applicationId;
    this.courier = courier;
    this.trackingNumber = trackingNumber;
    this.shippedAt = LocalDateTime.now();
    this.updatedAt = this.shippedAt;
  }

  /** 택배사·송장번호를 고칩니다. 값이 같으면 false를 돌려줍니다(알림을 다시 보내지 않기 위해). */
  public boolean change(String courier, String trackingNumber) {
    if (this.courier.equals(courier) && this.trackingNumber.equals(trackingNumber)) return false;
    this.courier = courier;
    this.trackingNumber = trackingNumber;
    this.updatedAt = LocalDateTime.now();
    return true;
  }
}
