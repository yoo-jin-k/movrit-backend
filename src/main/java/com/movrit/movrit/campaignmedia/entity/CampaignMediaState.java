package com.movrit.movrit.campaignmedia.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 버전 증가와 참조 치환은 캠페인 행의 쓰기 잠금 안에서만 수행합니다. */
@Entity
@Table(name = "campaign_media_state")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignMediaState extends BaseEntity {
  @Id
  @Column(name = "campaign_id")
  private Long campaignId;

  @Column(nullable = false)
  private long version;

  @Column(name = "representative_asset_id", length = 36)
  private String representativeAssetId;

  @Column(name = "detail_images_json", nullable = false, columnDefinition = "TEXT")
  private String detailImagesJson;

  @Column(name = "visit_location_json", columnDefinition = "TEXT")
  private String visitLocationJson;

  @Column(name = "retained_visit_location_json", columnDefinition = "TEXT")
  private String retainedVisitLocationJson;

  @Column(name = "legacy_representative_url", length = 1000)
  private String legacyRepresentativeUrl;

  @Column(name = "legacy_detail_url", length = 1000)
  private String legacyDetailUrl;

  public void replace(String representative, String details, String location) {
    if (location == null && visitLocationJson != null)
      retainedVisitLocationJson = visitLocationJson;
    if (location != null) retainedVisitLocationJson = null;
    representativeAssetId = representative;
    detailImagesJson = details;
    visitLocationJson = location;
    version++;
  }
}
