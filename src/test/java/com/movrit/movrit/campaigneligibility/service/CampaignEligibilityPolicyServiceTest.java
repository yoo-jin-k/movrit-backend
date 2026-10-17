package com.movrit.movrit.campaigneligibility.service;

import com.movrit.movrit.campaigneligibility.entity.CampaignEligibilityRequirement;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.CampaignEligibilityRequirementRepository;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.product.service.ProductService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.web.server.ResponseStatusException;

class CampaignEligibilityPolicyServiceTest {
    private final CampaignEligibilityRequirementRepository requirements = mock(CampaignEligibilityRequirementRepository.class);
    private final MemberChannelMetricRepository metrics = mock(MemberChannelMetricRepository.class);
    private final CampaignRepository campaigns = mock(CampaignRepository.class);
    private final ProductService products = mock(ProductService.class);
    private final CampaignEligibilityPolicyService service = new CampaignEligibilityPolicyService(
            requirements, metrics, campaigns, products
    );

    @Test
    void campaignWithoutRequirementAllowsApplicantWithoutMetric() {
        when(requirements.findByCampaignId(10L)).thenReturn(Optional.empty());

        assertThat(service.evaluate(1L, 10L).blockingReason()).isNull();
    }

    @Test
    void verifiedMetricBelowMinimumIsBlocked() {
        stubRequirement();
        when(metrics.findByMemberIdAndChannelTypeAndMetricType(1L, "INSTAGRAM", "FOLLOWER_COUNT"))
                .thenReturn(Optional.of(metric(499L, MetricVerificationStatus.VERIFIED)));

        var result = service.evaluate(1L, 10L);
        assertThat(result.blockingReason()).isEqualTo("CHANNEL_METRIC_BELOW_MINIMUM");
        assertThat(result.metricEvaluation().satisfied()).isFalse();
    }

    @Test
    void verifiedMetricEqualToMinimumIsAllowed() {
        stubRequirement();
        when(metrics.findByMemberIdAndChannelTypeAndMetricType(1L, "INSTAGRAM", "FOLLOWER_COUNT"))
                .thenReturn(Optional.of(metric(500L, MetricVerificationStatus.VERIFIED)));

        assertThat(service.evaluate(1L, 10L).blockingReason()).isNull();
    }

    @Test
    void missingMetricIsNotTreatedAsZero() {
        stubRequirement();
        when(metrics.findByMemberIdAndChannelTypeAndMetricType(1L, "INSTAGRAM", "FOLLOWER_COUNT"))
                .thenReturn(Optional.empty());

        assertThat(service.evaluate(1L, 10L).blockingReason()).isEqualTo("CHANNEL_METRIC_REQUIRED");
    }

    @Test
    void memberEnteredMetricMustBeVerifiedBeforeItCanQualify() {
        stubRequirement();
        when(metrics.findByMemberIdAndChannelTypeAndMetricType(1L, "INSTAGRAM", "FOLLOWER_COUNT"))
                .thenReturn(Optional.of(metric(500L, MetricVerificationStatus.UNVERIFIED)));

        assertThat(service.evaluate(1L, 10L).blockingReason()).isEqualTo("CHANNEL_METRIC_UNVERIFIED");
    }

    @Test
    void unsupportedChannelMetricPairIsRejected() {
        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaigns.findById(10L)).thenReturn(Optional.of(campaign));
        var request = new com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos.RequirementSaveRequest(
                "NAVER_BLOG", "VISITOR_COUNT", 100L, "DAILY", null
        );

        assertThatThrownBy(() -> service.save(1L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("현재 지원하는 캠페인 조건은 인스타그램 팔로워 수입니다");
    }

    @Test
    void staleRequirementVersionIsRejected() {
        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaigns.findById(10L)).thenReturn(Optional.of(campaign));
        CampaignEligibilityRequirement current = CampaignEligibilityRequirement.builder()
                .id(20L).campaign(campaign).channelType("INSTAGRAM").metricType("FOLLOWER_COUNT")
                .minimumValue(400L).version(3L).build();
        when(requirements.findByCampaignId(10L)).thenReturn(Optional.of(current));
        var request = new com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos.RequirementSaveRequest(
                "INSTAGRAM", "FOLLOWER_COUNT", 700L, null, 2L
        );

        assertThatThrownBy(() -> service.save(1L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("캠페인 조건이 변경되었습니다");
    }

    private void stubRequirement() {
        Campaign campaign = Campaign.builder().id(10L).build();
        CampaignEligibilityRequirement requirement = CampaignEligibilityRequirement.builder()
                .id(20L).campaign(campaign).channelType("INSTAGRAM").metricType("FOLLOWER_COUNT")
                .minimumValue(500L).build();
        when(requirements.findByCampaignId(10L)).thenReturn(Optional.of(requirement));
    }

    private MemberChannelMetric metric(long value, MetricVerificationStatus status) {
        return MemberChannelMetric.builder().value(value).verificationStatus(status).build();
    }
}
