package com.movrit.movrit.campaigneligibility.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
  public void updateByMember(Long value, String measurementPeriod, LocalDateTime measuredAt) {
    this.value = value;
    this.measurementPeriod = measurementPeriod;
    this.measuredAt = measuredAt;
    this.verificationStatus = MetricVerificationStatus.UNVERIFIED;
  }

  /** SNS 계정 연결로 받은 수치를 기록합니다. 서비스가 직접 알려준 값이므로 '확인됨(VERIFIED)'으로 둡니다. */
  public void verifyFromConnection(Long value, LocalDateTime measuredAt) {
    this.value = value;
    this.measuredAt = measuredAt;
    this.verificationStatus = MetricVerificationStatus.VERIFIED;
  }

  /** 운영자가 직접 등록한 채널을 반려하면 그 수치도 '반려(REJECTED)'로 둡니다. */
  public void markRejected() {
    this.verificationStatus = MetricVerificationStatus.REJECTED;
  }

  /** SNS 계정 연결을 해제하면 더 이상 확인된 값이 아니므로 '미확인'으로 되돌립니다. 수치는 그대로 둡니다. */
  public void markUnverified() {
    this.verificationStatus = MetricVerificationStatus.UNVERIFIED;
  }
}
