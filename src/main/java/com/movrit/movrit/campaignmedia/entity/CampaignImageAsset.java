package com.movrit.movrit.campaignmedia.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 새 이미지 관리 방식에서 업로드된 캠페인 이미지 한 장의 정보입니다. 원본 파일은 남기지 않고 검사·재인코딩한 표시본의 저장 위치·크기와, 올린 업체·연결된 캠페인만
 * 기록합니다.
 */
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

  /** 이 이미지가 연결된 캠페인입니다. 업로드 직후 아직 저장 전이면 비어 있습니다. */
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

  /** 이미지를 캠페인에 연결합니다. */
  public void attach(Long campaignId) {
    this.campaignId = campaignId;
  }

  /** 캠페인과의 연결을 끊습니다. 연결이 끊긴 이미지는 정리 대상이 됩니다. */
  public void detach() {
    this.campaignId = null;
  }
}
