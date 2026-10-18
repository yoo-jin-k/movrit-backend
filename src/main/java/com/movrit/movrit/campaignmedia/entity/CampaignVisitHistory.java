package com.movrit.movrit.campaignmedia.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** 주소 변경 증빙은 추가만 하며 기존 이력을 수정하지 않습니다. */
@Entity
@Table(name = "campaign_visit_history")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignVisitHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long campaignId;

  @Column(nullable = false)
  private Long actorMemberId;

  @Column(nullable = false)
  private long presentationVersion;

  @Column(columnDefinition = "TEXT")
  private String beforeLocationJson;

  @Column(columnDefinition = "TEXT")
  private String afterLocationJson;

  @Column(columnDefinition = "TEXT")
  private String legacyPlaceName;

  @Column(columnDefinition = "TEXT")
  private String legacyAddress;

  @Column(columnDefinition = "TEXT")
  private String legacyAfterPlaceName;

  @Column(columnDefinition = "TEXT")
  private String legacyAfterAddress;

  @Column(nullable = false)
  private LocalDateTime changedAt;
}
