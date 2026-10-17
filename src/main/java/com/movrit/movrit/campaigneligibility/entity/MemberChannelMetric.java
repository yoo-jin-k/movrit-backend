package com.movrit.movrit.campaigneligibility.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_channel_metric", uniqueConstraints = @UniqueConstraint(
        name = "uk_member_channel_metric", columnNames = {"member_id", "channel_type", "metric_type"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberChannelMetric extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    @Column(name = "channel_type", nullable = false, length = 40)
    private String channelType;
    @Column(name = "metric_type", nullable = false, length = 40)
    private String metricType;
    @Column(name = "metric_value", nullable = false)
    private Long value;
    @Column(name = "measurement_period", length = 40)
    private String measurementPeriod;
    @Column(name = "measured_at")
    private LocalDateTime measuredAt;
    @Enumerated(EnumType.STRING) @Column(name = "verification_status", nullable = false, length = 20)
    private MetricVerificationStatus verificationStatus;

    /** 사용자가 수치를 바꾸면 이전 확인 결과는 다시 사용할 수 없습니다. */
    public void updateByMember(Long value, String measurementPeriod, LocalDateTime measuredAt) {
        this.value = value;
        this.measurementPeriod = measurementPeriod;
        this.measuredAt = measuredAt;
        this.verificationStatus = MetricVerificationStatus.UNVERIFIED;
    }
}
