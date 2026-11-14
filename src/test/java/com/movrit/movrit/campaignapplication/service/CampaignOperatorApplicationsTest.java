package com.movrit.movrit.campaignapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.service.CampaignContentService;
import com.movrit.movrit.campaigneligibility.service.CampaignEligibilityPolicyService;
import com.movrit.movrit.campaigneligibility.service.MemberChannelMetricService;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignSelectionMode;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.service.CreatorService;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** 운영자 선정 목록이 선정 판단에 필요한 채널 지표·완료 이력을 함께 주고, 수락 전 연락처는 숨기는지 확인합니다. */
class CampaignOperatorApplicationsTest {

  private final CampaignRepository campaignRepository = mock(CampaignRepository.class);
  private final CampaignApplicationFormRepository formRepository =
      mock(CampaignApplicationFormRepository.class);
  private final CampaignApplicationRepository applicationRepository =
      mock(CampaignApplicationRepository.class);
  private final MemberService memberService = mock(MemberService.class);

  @Test
  void operatorListIncludesCompletedCountAndMetricsButHidesContactBeforeAcceptance() {
    CampaignApplication application = application(CampaignApplicationStatus.SUBMITTED);
    when(applicationRepository.findAllByCampaignSelectionModeOrderByIdDesc(
            CampaignSelectionMode.OPERATOR))
        .thenReturn(List.of(application));
    when(applicationRepository.countCompletedCampaignsByCreatorIds(any()))
        .thenReturn(List.of(completed(20L, 3L)));
    when(applicationRepository.findMetricSummariesByMemberIds(any()))
        .thenReturn(List.of(metric(7L)));

    List<CampaignApplicationDtos.Response> result = service().findOperatorApplications(1L);

    assertThat(result).hasSize(1);
    CampaignApplicationDtos.Response response = result.get(0);
    assertThat(response.completedCampaignCount()).isEqualTo(3L);
    assertThat(response.creatorMetrics()).hasSize(1);
    assertThat(response.creatorMetrics().get(0).metricName()).isEqualTo("팔로워 수");
    assertThat(response.creatorMetrics().get(0).value()).isEqualTo(1200L);
    // 참여 수락 전이므로 배송 연락처는 내려가지 않습니다.
    assertThat(response.shippingContact()).isNull();
  }

  @Test
  void creatorWithoutHistoryGetsZeroAndEmptyMetrics() {
    when(applicationRepository.findAllByCampaignSelectionModeOrderByIdDesc(
            CampaignSelectionMode.OPERATOR))
        .thenReturn(List.of(application(CampaignApplicationStatus.SUBMITTED)));
    when(applicationRepository.countCompletedCampaignsByCreatorIds(any())).thenReturn(List.of());
    when(applicationRepository.findMetricSummariesByMemberIds(any())).thenReturn(List.of());

    CampaignApplicationDtos.Response response = service().findOperatorApplications(1L).get(0);

    assertThat(response.completedCampaignCount()).isZero();
    assertThat(response.creatorMetrics()).isEmpty();
  }

  @Test
  void nonAdminIsRejectedBeforeReadingApplications() {
    when(memberService.getAdmin(2L))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."));

    assertThatThrownBy(() -> service().findOperatorApplications(2L))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(error -> ((ResponseStatusException) error).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    verify(applicationRepository, never()).findAllByCampaignSelectionModeOrderByIdDesc(any());
  }

  private CampaignApplication application(CampaignApplicationStatus status) {
    Member member = Member.builder().id(7L).nickname("minji").build();
    Creator creator =
        Creator.builder()
            .id(20L)
            .member(member)
            .nickname("민지")
            .snsUrl("https://instagram.com/minji")
            .status("ACTIVE")
            .build();
    Campaign campaign =
        Campaign.builder()
            .brand(Brand.builder().id(99L).name("테스트 브랜드").build())
            .id(10L)
            .title("운영자 선정 캠페인")
            .selectionMode(CampaignSelectionMode.OPERATOR)
            .build();
    return CampaignApplication.builder()
        .id(12L)
        .campaign(campaign)
        .creator(creator)
        .fulfillmentType(FulfillmentType.DELIVERY)
        .status(status)
        .recipientName("김민지")
        .recipientPhone("010-1234-5678")
        .answersJson("[]")
        .agreementAcceptancesJson("[]")
        .formVersion(1L)
        .submittedAt(LocalDateTime.of(2026, 11, 1, 10, 0))
        .build();
  }

  private static CampaignApplicationRepository.CompletedCampaignCount completed(
      Long creatorId, Long count) {
    return new CampaignApplicationRepository.CompletedCampaignCount() {
      @Override
      public Long getCreatorId() {
        return creatorId;
      }

      @Override
      public Long getCompletedCount() {
        return count;
      }
    };
  }

  private static CampaignApplicationRepository.CreatorMetricSummary metric(Long memberId) {
    return new CampaignApplicationRepository.CreatorMetricSummary() {
      @Override
      public Long getMemberId() {
        return memberId;
      }

      @Override
      public String getChannelType() {
        return "INSTAGRAM";
      }

      @Override
      public String getMetricType() {
        return "FOLLOWER_COUNT";
      }

      @Override
      public Long getMetricValue() {
        return 1200L;
      }

      @Override
      public String getMeasurementPeriod() {
        return null;
      }

      @Override
      public LocalDateTime getMeasuredAt() {
        return LocalDateTime.of(2026, 11, 1, 9, 0);
      }

      @Override
      public String getVerificationStatus() {
        return "VERIFIED";
      }
    };
  }

  private CampaignApplicationService service() {
    CampaignApplicationJson json = new CampaignApplicationJson(new ObjectMapper());
    CampaignApplicationFormService forms =
        new CampaignApplicationFormService(
            campaignRepository, formRepository, mock(CampaignOwnershipService.class), json);
    return new CampaignApplicationService(
        campaignRepository,
        forms,
        new CampaignApplicationValidator(forms),
        applicationRepository,
        mock(CreatorService.class),
        memberService,
        mock(CampaignEligibilityPolicyService.class),
        mock(MemberChannelMetricService.class),
        mock(SellerProfileService.class),
        mock(CampaignOwnershipService.class),
        json,
        new CampaignApplicationResponses(json),
        mock(NotificationService.class),
        mock(AuditLogService.class),
        mock(com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService.class),
        mock(CampaignContentService.class),
        mock(com.movrit.movrit.participation.service.ActivityService.class));
  }
}
