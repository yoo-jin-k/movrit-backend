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
 * 캠페인 하나의 현재 이미지·방문 장소 구성을 담는 상태입니다(새 이미지 관리 방식). 저장할 때마다 version이 1 올라가며, 동시 수정 충돌을 막기 위해 캠페인 행을
 * 잠근 상태에서만 바꿉니다. 이 데이터가 있는 캠페인은 새 방식으로 전환된 것으로 봅니다.
 */
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

  /** 방문형을 해제할 때 지운 방문 장소를 잠시 보관해 두는 칸입니다. 다시 방문형으로 바꾸면 비웁니다. */
  @Column(name = "retained_visit_location_json", columnDefinition = "TEXT")
  private String retainedVisitLocationJson;

  /** 새 방식 전환 전에 쓰던 대표 이미지 주소입니다. 기록 보존용입니다. */
  @Column(name = "legacy_representative_url", length = 1000)
  private String legacyRepresentativeUrl;

  @Column(name = "legacy_detail_url", length = 1000)
  private String legacyDetailUrl;

  /**
   * 대표 이미지·상세 이미지 목록·방문 장소를 한 번에 새 값으로 바꾸고 버전을 1 올립니다. 방문 장소를 비우면 이전 장소를 retainedVisitLocationJson에
   * 보관합니다.
   */
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
