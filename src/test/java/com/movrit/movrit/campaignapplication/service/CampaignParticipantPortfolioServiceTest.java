package com.movrit.movrit.campaignapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.repository.CampaignContentLinkRepository;
import com.movrit.movrit.campaigncontent.repository.CampaignContentSubmissionRepository;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.channelconnect.service.ChannelConnectionService;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.product.entity.Product;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CampaignParticipantPortfolioServiceTest {

  @Test
  void returnsOnlyPortfolioFieldsWithVersionMatchedQuestionLabel() {
    CampaignApplicationRepository applications = mock(CampaignApplicationRepository.class);
    CampaignApplicationFormRepository forms = mock(CampaignApplicationFormRepository.class);
    CampaignContentSubmissionRepository submissions =
        mock(CampaignContentSubmissionRepository.class);
    CampaignContentLinkRepository links = mock(CampaignContentLinkRepository.class);
    MemberChannelMetricRepository metrics = mock(MemberChannelMetricRepository.class);
    CampaignOwnershipService products = mock(CampaignOwnershipService.class);

    Member member = Member.builder().id(7L).nickname("account_name").build();
    Creator creator =
        Creator.builder()
            .id(8L)
            .member(member)
            .nickname("channel_name")
            .snsUrl("https://instagram.com/channel")
            .status("ACTIVE")
            .build();
    Product product = Product.builder().id(9L).build();
    Preorder preorder = Preorder.builder().id(10L).product(product).build();
    Campaign campaign =
        Campaign.builder()
            .brand(Brand.builder().id(99L).name("테스트 브랜드").build())
            .id(11L)
            .preorder(preorder)
            .title("캠페인")
            .build();
    CampaignApplication application =
        CampaignApplication.builder()
            .id(12L)
            .campaign(campaign)
            .creator(creator)
            .fulfillmentType(FulfillmentType.DELIVERY)
            .status(CampaignApplicationStatus.SUBMITTED)
            .answersJson("[{\"questionId\":\"reason\",\"value\":\"지원합니다\"}]")
            .agreementAcceptancesJson("[]")
            .formVersion(3L)
            .build();
    CampaignApplicationForm form =
        CampaignApplicationForm.builder()
            .campaign(campaign)
            .version(3L)
            .questionsJson(
                "[{\"id\":\"reason\",\"label\":\"지원 동기\",\"required\":true,\"type\":\"TEXT\",\"options\":[],\"maxLength\":100}]")
            .agreementsJson("[]")
            .allowedFulfillmentTypesJson("[\"DELIVERY\"]")
            .build();
    MemberChannelMetric metric =
        MemberChannelMetric.builder()
            .member(member)
            .channelType("INSTAGRAM")
            .metricType("FOLLOWER_COUNT")
            .value(100L)
            .measuredAt(LocalDateTime.now())
            .verificationStatus(MetricVerificationStatus.VERIFIED)
            .build();

    when(applications.findPortfolioById(12L)).thenReturn(Optional.of(application));
    when(forms.findByCampaignId(11L)).thenReturn(Optional.of(form));
    when(metrics.findAllByMemberIdOrderById(7L)).thenReturn(List.of(metric));
    when(submissions
            .findAllByApplicationCreatorIdAndApplicationStatusAndStatusOrderByReviewedAtDesc(
                8L,
                CampaignApplicationStatus.COMPLETED,
                com.movrit.movrit.campaigncontent.entity.ContentSubmissionStatus.APPROVED))
        .thenReturn(List.of());

    var result =
        new CampaignParticipantPortfolioService(
                applications,
                forms,
                submissions,
                links,
                metrics,
                products,
                new ObjectMapper(),
                mock(ChannelConnectionService.class))
            .find(20L, 12L);

    verify(products)
        .requireOwner(
            org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.any(Campaign.class));
    assertThat(result.profile().channelNickname()).isEqualTo("channel_name");
    assertThat(result.profile().accountNickname()).isEqualTo("account_name");
    assertThat(result.profile().nicknameConsistent()).isFalse();
    assertThat(result.metrics().get(0).source()).isEqualTo("UNKNOWN");
    assertThat(result.applicationAnswers().get(0).questionLabel()).isEqualTo("지원 동기");
    assertThat(result.completedCampaignCount()).isZero();
  }
}
