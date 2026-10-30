package com.movrit.movrit.campaigneligibility.entity;

import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캠페인별 신청 조건 한 가지(예: 인스타그램 팔로워 500명 이상)입니다. 조건이 없는 캠페인은 이 데이터가 없고, 캠페인 하나에 조건은 최대 하나입니다. version은
 * 신청 당시 조건과 현재 조건을 구분하고 동시 수정을 막는 데 사용합니다.
 */
@Entity
@Table(name = "campaign_eligibility_requirement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CampaignEligibilityRequirement extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false, unique = true)
  private Campaign campaign;

  @Column(name = "channel_type", nullable = false, length = 40)
  private String channelType;

  @Column(name = "metric_type", nullable = false, length = 40)
  private String metricType;

  @Column(name = "minimum_value", nullable = false)
  private Long minimumValue;

  /** 방문자 수처럼 기간이 필요한 지표의 집계 기간입니다. 팔로워 수처럼 기간이 없으면 비어 있습니다. */
  @Column(name = "measurement_period", length = 40)
  private String measurementPeriod;

  @Version
  @Column(nullable = false)
  private Long version;

  /** 업체가 신청 조건의 채널·지표·최소값·기간을 바꿉니다. 저장되면 version이 자동으로 1 올라갑니다. */
  public void update(
      String channelType, String metricType, Long minimumValue, String measurementPeriod) {
    this.channelType = channelType;
    this.metricType = metricType;
    this.minimumValue = minimumValue;
    this.measurementPeriod = measurementPeriod;
  }
}
