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
        CampaignEligibilityRequirement requirement = requirementRepository.findByCampaignId(campaignId)
                .orElseGet(() -> CampaignEligibilityRequirement.builder().campaign(campaign).build());
        requirement.update(channelType, metricType, request.minimumValue(), normalize(request.measurementPeriod()));
        return response(requirementRepository.save(requirement));
    }

    @Transactional
    public void delete(Long sellerMemberId, Long campaignId) {
        ownedCampaign(sellerMemberId, campaignId);
        requirementRepository.deleteByCampaignId(campaignId);
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
                requirement.getMetricType(), requirement.getMinimumValue(), requirement.getMeasurementPeriod()
        );
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
