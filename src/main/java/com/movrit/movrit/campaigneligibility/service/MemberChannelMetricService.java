package com.movrit.movrit.campaigneligibility.service;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.creator.repository.CreatorRepository;
import com.movrit.movrit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberChannelMetricService {
    private final MemberChannelMetricRepository repository;
    private final MemberService memberService;
    private final CreatorRepository creatorRepository;

    public CampaignEligibilityDtos.ChannelProfileResponse findMine(Long memberId) {
        List<CampaignEligibilityDtos.MetricResponse> metrics = repository.findAllByMemberIdOrderById(memberId)
                .stream().map(this::response).toList();
        return new CampaignEligibilityDtos.ChannelProfileResponse(
                creatorRepository.existsByMemberId(memberId) || !metrics.isEmpty(),
                metrics
        );
    }

    @Transactional
    public CampaignEligibilityDtos.ChannelProfileResponse saveMine(
            Long memberId,
            CampaignEligibilityDtos.MetricSaveRequest request
    ) {
        String channelType = request.channelType().trim().toUpperCase();
        String metricType = request.metricType().trim().toUpperCase();
        MemberChannelMetric metric = repository
                .findByMemberIdAndChannelTypeAndMetricType(memberId, channelType, metricType)
                .orElseGet(() -> MemberChannelMetric.builder()
                        .member(memberService.getMember(memberId))
                        .channelType(channelType)
                        .metricType(metricType)
                        .value(request.value())
                        .measurementPeriod(normalize(request.measurementPeriod()))
                        .measuredAt(request.measuredAt())
                        .verificationStatus(MetricVerificationStatus.UNVERIFIED)
                        .build());
        if (metric.getId() != null) {
            metric.updateByMember(request.value(), normalize(request.measurementPeriod()), request.measuredAt());
        }
        repository.save(metric);
        return findMine(memberId);
    }

    private CampaignEligibilityDtos.MetricResponse response(MemberChannelMetric metric) {
        return new CampaignEligibilityDtos.MetricResponse(
                metric.getId(), metric.getChannelType(), metric.getMetricType(), metric.getValue(),
                metric.getMeasurementPeriod(), metric.getMeasuredAt(), metric.getVerificationStatus()
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
