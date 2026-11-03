package com.movrit.movrit.participation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업체 미선정 기록입니다. 업체 직접 선정 캠페인에 신청자가 있었는데 선정 마감(선정 발표일, 없으면 모집 마감)까지 한 명도 선정하지 않으면 캠페인마다 한 번 남깁니다.
 *
 * <p>업체 대시보드와 운영자 화면에서 횟수를 보여 주며, 참여자가 기다리기만 하다 끝나는 일을 줄이기 위한 기록입니다.
 */
@Entity
@Table(name = "campaign_unselected_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignUnselectedRecord {
  @Id
  @Column(name = "campaign_id")
  private Long campaignId;

  @Column(name = "seller_member_id", nullable = false)
  private Long sellerMemberId;

  @Column(name = "application_count", nullable = false)
  private int applicationCount;

  @Column(name = "recorded_at", nullable = false)
  private LocalDateTime recordedAt;

  public CampaignUnselectedRecord(Long campaignId, Long sellerMemberId, int applicationCount) {
    this.campaignId = campaignId;
    this.sellerMemberId = sellerMemberId;
    this.applicationCount = applicationCount;
    this.recordedAt = LocalDateTime.now();
  }
}
