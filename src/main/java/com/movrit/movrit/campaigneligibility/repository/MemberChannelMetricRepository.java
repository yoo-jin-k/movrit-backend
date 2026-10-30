package com.movrit.movrit.campaigneligibility.repository;

import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원이 입력한 채널 지표를 저장하고 조회합니다. */
public interface MemberChannelMetricRepository extends JpaRepository<MemberChannelMetric, Long> {
  /** 회원이 등록한 채널 지표를 등록 순으로 조회합니다. */
  List<MemberChannelMetric> findAllByMemberIdOrderById(Long memberId);

  /** 회원의 특정 채널·지표 값을 찾습니다. 조합마다 하나만 존재합니다. */
  Optional<MemberChannelMetric> findByMemberIdAndChannelTypeAndMetricType(
      Long memberId, String channelType, String metricType);
}
