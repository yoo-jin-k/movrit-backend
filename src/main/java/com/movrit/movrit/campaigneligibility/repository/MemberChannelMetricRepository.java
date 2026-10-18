package com.movrit.movrit.campaigneligibility.repository;

import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberChannelMetricRepository extends JpaRepository<MemberChannelMetric, Long> {
  List<MemberChannelMetric> findAllByMemberIdOrderById(Long memberId);

  Optional<MemberChannelMetric> findByMemberIdAndChannelTypeAndMetricType(
      Long memberId, String channelType, String metricType);
}
