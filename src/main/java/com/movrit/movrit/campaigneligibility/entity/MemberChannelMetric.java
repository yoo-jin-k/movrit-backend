package com.movrit.movrit.campaigneligibility.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "member_channel_metric",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_member_channel_metric",
            columnNames = {"member_id", "channel_type", "metric_type"}))
/**
 * 회원이 직접 입력한 채널 지표 한 개(예: 인스타그램 팔로워 1,200명)입니다. 회원·채널·지표 조합마다 하나만 저장되며, 실제 SNS 연동 값이 아니라 입력값과 확인
 * 상태를 함께 관리합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberChannelMetric extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "channel_type", nullable = false, length = 40)
  private String channelType;

  @Column(name = "metric_type", nullable = false, length = 40)
  private String metricType;

  @Column(name = "metric_value", nullable = false)
  private Long value;

  @Column(name = "measurement_period", length = 40)
  private String measurementPeriod;

  /** 회원이 수치를 확인한 시각입니다. */
  @Column(name = "measured_at")
  private LocalDateTime measuredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false, length = 20)
  private MetricVerificationStatus verificationStatus;

  /** 회원이 지표 수치를 고칩니다. 수치가 바뀌면 예전 확인 결과를 믿을 수 없으므로 확인 상태를 '미확인(UNVERIFIED)'으로 되돌립니다. */
  /** 사용자가 수치를 바꾸면 이전 확인 결과는 다시 사용할 수 없습니다. */
  public void updateByMember(Long value, String measurementPeriod, LocalDateTime measuredAt) {
    this.value = value;
    this.measurementPeriod = measurementPeriod;
    this.measuredAt = measuredAt;
    this.verificationStatus = MetricVerificationStatus.UNVERIFIED;
  }
}
