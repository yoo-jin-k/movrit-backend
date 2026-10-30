package com.movrit.movrit.campaignmedia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 캠페인 방문 장소가 바뀐 기록 한 건입니다. 누가 언제 무엇을 무엇으로 바꿨는지 남깁니다. 기록은 추가만 하고 수정·삭제하지 않아 분쟁 시 증빙으로 쓸 수 있습니다. */
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

  /** 예전 방식(장소 이름·주소 칸)으로 바뀐 경우의 변경 전 값입니다. */
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
