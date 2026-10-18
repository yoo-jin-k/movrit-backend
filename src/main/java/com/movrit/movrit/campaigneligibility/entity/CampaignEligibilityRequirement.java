package com.movrit.movrit.campaigneligibility.entity;

import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

  @Column(name = "measurement_period", length = 40)
  private String measurementPeriod;

  @Version
  @Column(nullable = false)
  private Long version;

  public void update(
      String channelType, String metricType, Long minimumValue, String measurementPeriod) {
    this.channelType = channelType;
    this.metricType = metricType;
    this.minimumValue = minimumValue;
    this.measurementPeriod = measurementPeriod;
  }
}
