package com.movrit.movrit.campaignmedia.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

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
