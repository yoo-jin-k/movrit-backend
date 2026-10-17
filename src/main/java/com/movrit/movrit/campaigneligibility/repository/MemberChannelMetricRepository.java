package com.movrit.movrit.campaigneligibility.repository;

import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberChannelMetricRepository extends JpaRepository<MemberChannelMetric, Long> {
    List<MemberChannelMetric> findAllByMemberIdOrderById(Long memberId);
    Optional<MemberChannelMetric> findByMemberIdAndChannelTypeAndMetricType(Long memberId, String channelType, String metricType);
}
