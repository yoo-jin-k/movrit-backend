package com.movrit.movrit.campaigneligibility.service;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.creator.repository.CreatorRepository;
import com.movrit.movrit.member.service.MemberService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원이 자기 채널 지표(팔로워 수 등)를 조회·입력하는 기능을 맡습니다. 본인 데이터만 다룹니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberChannelMetricService {
  private final MemberChannelMetricRepository repository;
  private final MemberService memberService;
  private final CreatorRepository creatorRepository;

  /** 내 채널 지표 목록을 조회합니다. 활동 프로필이 있거나 지표를 하나라도 입력했으면 '채널 프로필 있음'으로 표시합니다. */
  public CampaignEligibilityDtos.ChannelProfileResponse findMine(Long memberId) {
    List<CampaignEligibilityDtos.MetricResponse> metrics =
        repository.findAllByMemberIdOrderById(memberId).stream().map(this::response).toList();
    return new CampaignEligibilityDtos.ChannelProfileResponse(
        creatorRepository.existsByMemberId(memberId) || !metrics.isEmpty(), metrics);
  }

  /**
   * 내 채널 지표를 새로 입력하거나 고칩니다. 채널·지표 이름은 대문자로 맞춰 저장합니다. 새로 넣든 고치든 확인 상태는 '미확인'이 되며, 저장 후 최신 목록을 돌려줍니다.
   */
  @Transactional
  public CampaignEligibilityDtos.ChannelProfileResponse saveMine(
      Long memberId, CampaignEligibilityDtos.MetricSaveRequest request) {
    String channelType = request.channelType().trim().toUpperCase();
    String metricType = request.metricType().trim().toUpperCase();
    MemberChannelMetric metric =
        repository
            .findByMemberIdAndChannelTypeAndMetricType(memberId, channelType, metricType)
            .orElseGet(
                () ->
                    MemberChannelMetric.builder()
                        .member(memberService.getMember(memberId))
                        .channelType(channelType)
                        .metricType(metricType)
                        .value(request.value())
                        .measurementPeriod(normalize(request.measurementPeriod()))
                        .measuredAt(request.measuredAt())
                        .verificationStatus(MetricVerificationStatus.UNVERIFIED)
                        .build());
    if (metric.getId() != null) {
      metric.updateByMember(
          request.value(), normalize(request.measurementPeriod()), request.measuredAt());
    }
    repository.save(metric);
    return findMine(memberId);
  }

  private CampaignEligibilityDtos.MetricResponse response(MemberChannelMetric metric) {
    return new CampaignEligibilityDtos.MetricResponse(
        metric.getId(),
        metric.getChannelType(),
        metric.getMetricType(),
        metric.getValue(),
        metric.getMeasurementPeriod(),
        metric.getMeasuredAt(),
        metric.getVerificationStatus());
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
