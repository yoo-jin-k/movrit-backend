package com.movrit.movrit.campaigneligibility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaigneligibility.entity.CampaignEligibilityRequirement;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.CampaignEligibilityRequirementRepository;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CampaignEligibilityPolicyServiceTest {
  private final CampaignEligibilityRequirementRepository requirements =
      mock(CampaignEligibilityRequirementRepository.class);
  private final MemberChannelMetricRepository metrics = mock(MemberChannelMetricRepository.class);
  private final CampaignRepository campaigns = mock(CampaignRepository.class);
  private final CampaignOwnershipService products = mock(CampaignOwnershipService.class);
  private final CampaignEligibilityPolicyService service =
      new CampaignEligibilityPolicyService(requirements, metrics, campaigns, products);

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
    var request =
        new com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos
            .RequirementSaveRequest("NAVER_BLOG", "VISITOR_COUNT", 100L, "DAILY", null);

    assertThatThrownBy(() -> service.save(1L, 10L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("지원하는 캠페인 조건은 인스타그램 팔로워 수와 네이버 블로그 일평균 방문자입니다");
  }

  @Test
  void staleRequirementVersionIsRejected() {
    Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
    when(campaigns.findById(10L)).thenReturn(Optional.of(campaign));
    CampaignEligibilityRequirement current =
        CampaignEligibilityRequirement.builder()
            .id(20L)
            .campaign(campaign)
            .channelType("INSTAGRAM")
            .metricType("FOLLOWER_COUNT")
            .minimumValue(400L)
            .version(3L)
            .build();
    when(requirements.findByCampaignId(10L)).thenReturn(Optional.of(current));
    var request =
        new com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos
            .RequirementSaveRequest("INSTAGRAM", "FOLLOWER_COUNT", 700L, null, 2L);

    assertThatThrownBy(() -> service.save(1L, 10L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("캠페인 조건이 변경되었습니다");
  }

  @Test
  void visitorConditionUsesRecentFourDayPeriod() {
    assertThat(
            CampaignEligibilityPolicyService.supportedPeriod("NAVER_BLOG", "DAILY_VISITORS", null))
        .isEqualTo("RECENT_4_DAYS");
    assertThatThrownBy(
            () ->
                CampaignEligibilityPolicyService.supportedPeriod(
                    "NAVER_BLOG", "DAILY_VISITORS", "MONTHLY"))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                CampaignEligibilityPolicyService.supportedPeriod(
                    "INSTAGRAM", "FOLLOWER_COUNT", "RECENT_4_DAYS"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void visitorCountCheckedOverADayAgoMustBeCheckedAgain() {
    stubVisitorRequirement();
    when(metrics.findByMemberIdAndChannelTypeAndMetricType(1L, "NAVER_BLOG", "DAILY_VISITORS"))
        .thenReturn(Optional.of(visitorMetric(500L, java.time.LocalDateTime.now().minusHours(25))));

    assertThat(service.evaluate(1L, 10L).blockingReason()).isEqualTo("CHANNEL_METRIC_STALE");
  }

  @Test
  void freshVisitorCountIsEvaluatedAndKeepsMeasuredAt() {
    stubVisitorRequirement();
    java.time.LocalDateTime checkedAt = java.time.LocalDateTime.now().minusMinutes(5);
    when(metrics.findByMemberIdAndChannelTypeAndMetricType(1L, "NAVER_BLOG", "DAILY_VISITORS"))
        .thenReturn(Optional.of(visitorMetric(120L, checkedAt)));

    var result = service.evaluate(1L, 10L);
    assertThat(result.blockingReason()).isNull();
    assertThat(result.measuredAt()).isEqualTo(checkedAt);
    assertThat(result.metricEvaluation().actualValue()).isEqualTo(120L);
  }

  @Test
  void freshVisitorCountBelowMinimumIsBlocked() {
    stubVisitorRequirement();
    when(metrics.findByMemberIdAndChannelTypeAndMetricType(1L, "NAVER_BLOG", "DAILY_VISITORS"))
        .thenReturn(Optional.of(visitorMetric(99L, java.time.LocalDateTime.now())));

    assertThat(service.evaluate(1L, 10L).blockingReason())
        .isEqualTo("CHANNEL_METRIC_BELOW_MINIMUM");
  }

  private void stubVisitorRequirement() {
    Campaign campaign =
        Campaign.builder().brand(Brand.builder().id(99L).name("테스트 브랜드").build()).id(10L).build();
    CampaignEligibilityRequirement requirement =
        CampaignEligibilityRequirement.builder()
            .id(21L)
            .campaign(campaign)
            .channelType("NAVER_BLOG")
            .metricType("DAILY_VISITORS")
            .minimumValue(100L)
            .measurementPeriod("RECENT_4_DAYS")
            .build();
    when(requirements.findByCampaignId(10L)).thenReturn(Optional.of(requirement));
  }

  private MemberChannelMetric visitorMetric(long value, java.time.LocalDateTime measuredAt) {
    return MemberChannelMetric.builder()
        .value(value)
        .measurementPeriod("RECENT_4_DAYS")
        .measuredAt(measuredAt)
        .verificationStatus(MetricVerificationStatus.VERIFIED)
        .build();
  }

  private void stubRequirement() {
    Campaign campaign =
        Campaign.builder().brand(Brand.builder().id(99L).name("테스트 브랜드").build()).id(10L).build();
    CampaignEligibilityRequirement requirement =
        CampaignEligibilityRequirement.builder()
            .id(20L)
            .campaign(campaign)
            .channelType("INSTAGRAM")
            .metricType("FOLLOWER_COUNT")
            .minimumValue(500L)
            .build();
    when(requirements.findByCampaignId(10L)).thenReturn(Optional.of(requirement));
  }

  private MemberChannelMetric metric(long value, MetricVerificationStatus status) {
    return MemberChannelMetric.builder().value(value).verificationStatus(status).build();
  }
}
