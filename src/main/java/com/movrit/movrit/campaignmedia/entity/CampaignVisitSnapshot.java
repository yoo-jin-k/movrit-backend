package com.movrit.movrit.campaignmedia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참여자가 신청한 순간의 방문 장소·방문 안내를 그대로 저장한 사본입니다. 업체가 나중에 장소를 바꿔도 참여자가 신청할 때 본 정보를 확인할 수 있습니다. 신청 한 건당
 * 하나입니다.
 */
/** 신청 당시의 방문 정보를 별도 보존하며 기존 신청 조건 스냅샷을 변경하지 않습니다. */
@Entity
@Table(name = "campaign_visit_snapshot")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignVisitSnapshot {
  @Id private Long applicationId;

  @Column(nullable = false)
  private Long campaignId;

  @Column(nullable = false)
  private long presentationVersion;

  @Column(columnDefinition = "TEXT")
  private String locationJson;

  @Column(columnDefinition = "TEXT")
  private String visitGuide;

  @Column(columnDefinition = "TEXT")
  private String legacyPlaceName;

  @Column(columnDefinition = "TEXT")
  private String legacyAddress;

  @Column(nullable = false)
  private LocalDateTime capturedAt;
}
