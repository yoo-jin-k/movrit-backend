package com.movrit.movrit.campaigneligibility.service;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.entity.CampaignEligibilityRequirement;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.CampaignEligibilityRequirementRepository;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignEligibilityPolicyService {
    private final CampaignEligibilityRequirementRepository requirementRepository;
    private final MemberChannelMetricRepository metricRepository;
    private final CampaignRepository campaignRepository;
    private final ProductService productService;

    public record Evaluation(
            CampaignEligibilityDtos.RequirementResponse requirement,
            CampaignEligibilityDtos.MetricEvaluation metricEvaluation,
            String blockingReason
    ) {}

    public Evaluation evaluate(Long memberId, Long campaignId) {
        Optional<CampaignEligibilityRequirement> requirement = requirementRepository.findByCampaignId(campaignId);
        if (requirement.isEmpty()) {
            return new Evaluation(null, null, null);
        }
        CampaignEligibilityRequirement value = requirement.get();
        Optional<MemberChannelMetric> metric = metricRepository.findByMemberIdAndChannelTypeAndMetricType(
                memberId, value.getChannelType(), value.getMetricType()
        );
        if (metric.isEmpty()) {
            return result(value, null, null, "CHANNEL_METRIC_REQUIRED");
        }
        MemberChannelMetric actual = metric.get();
        if (actual.getVerificationStatus() != MetricVerificationStatus.VERIFIED) {
            return result(value, actual.getValue(), actual.getVerificationStatus(), "CHANNEL_METRIC_UNVERIFIED");
        }
        if (actual.getValue() < value.getMinimumValue()) {
            return result(value, actual.getValue(), actual.getVerificationStatus(), "CHANNEL_METRIC_BELOW_MINIMUM");
        }
        return result(value, actual.getValue(), actual.getVerificationStatus(), null);
    }

    @Transactional
    public CampaignEligibilityDtos.RequirementResponse save(
            Long sellerMemberId,
            Long campaignId,
            CampaignEligibilityDtos.RequirementSaveRequest request
    ) {
        Campaign campaign = ownedCampaign(sellerMemberId, campaignId);
        String channelType = request.channelType().trim().toUpperCase();
        String metricType = request.metricType().trim().toUpperCase();
        validateSupportedMetric(channelType, metricType, request.measurementPeriod());
        CampaignEligibilityRequirement requirement = requirementRepository.findByCampaignId(campaignId)
                .orElseGet(() -> CampaignEligibilityRequirement.builder().campaign(campaign).build());
        if (requirement.getId() == null && request.expectedVersion() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
        }
        if (requirement.getId() != null && !requirement.getVersion().equals(request.expectedVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
        }
        requirement.update(channelType, metricType, request.minimumValue(), normalize(request.measurementPeriod()));
        // @Version 값은 flush 시점에 증가하므로 응답을 만들기 전에 DB 반영을 끝냅니다.
        return response(requirementRepository.saveAndFlush(requirement));
    }

    @Transactional
    public void delete(Long sellerMemberId, Long campaignId, Long expectedVersion) {
        ownedCampaign(sellerMemberId, campaignId);
        CampaignEligibilityRequirement requirement = requirementRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인 조건이 없습니다."));
        if (!requirement.getVersion().equals(expectedVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
        }
        requirementRepository.delete(requirement);
    }

    public CampaignEligibilityDtos.RequirementResponse find(Long campaignId) {
        return requirementRepository.findByCampaignId(campaignId).map(this::response).orElse(null);
    }

    private Evaluation result(CampaignEligibilityRequirement requirement, Long actual,
                              MetricVerificationStatus status, String reason) {
        return new Evaluation(
                response(requirement),
                new CampaignEligibilityDtos.MetricEvaluation(
                        requirement.getChannelType(), requirement.getMetricType(), requirement.getMinimumValue(),
                        actual, status, reason == null, reason
                ),
                reason
        );
    }

    private CampaignEligibilityDtos.RequirementResponse response(CampaignEligibilityRequirement requirement) {
        return new CampaignEligibilityDtos.RequirementResponse(
                requirement.getId(), requirement.getCampaign().getId(), requirement.getChannelType(),
                requirement.getMetricType(), requirement.getMinimumValue(), requirement.getMeasurementPeriod(),
                requirement.getVersion()
        );
    }

    private void validateSupportedMetric(String channelType, String metricType, String measurementPeriod) {
        if (!"INSTAGRAM".equals(channelType) || !"FOLLOWER_COUNT".equals(metricType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 지원하는 캠페인 조건은 인스타그램 팔로워 수입니다.");
        }
        if (measurementPeriod != null && !measurementPeriod.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "팔로워 수 조건에는 집계 기간을 입력하지 않습니다.");
        }
    }

    private Campaign ownedCampaign(Long memberId, Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
        productService.getOwnedProduct(memberId, campaign.getPreorder().getProduct().getId());
        return campaign;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
