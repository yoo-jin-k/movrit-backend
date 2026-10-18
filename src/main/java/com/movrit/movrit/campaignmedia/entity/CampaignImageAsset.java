package com.movrit.movrit.campaignmedia.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 원본 대신 검증된 표시본과 현재 소유·연결 정보만 보관합니다. */
@Entity
@Table(name = "campaign_image_asset")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignImageAsset extends BaseEntity {
  @Id
  @Column(length = 36)
  private String id;

  @Column(name = "owner_member_id", nullable = false)
  private Long ownerMemberId;

  @Column(name = "campaign_id")
  private Long campaignId;

  @Column(name = "storage_key", nullable = false, length = 100, unique = true)
  private String storageKey;

  @Column(name = "content_type", nullable = false, length = 30)
  private String contentType;

  @Column(name = "byte_size", nullable = false)
  private long byteSize;

  @Column(nullable = false)
  private int width;

  @Column(nullable = false)
  private int height;

  public void attach(Long campaignId) {
    this.campaignId = campaignId;
  }

  public void detach() {
    this.campaignId = null;
  }
}
