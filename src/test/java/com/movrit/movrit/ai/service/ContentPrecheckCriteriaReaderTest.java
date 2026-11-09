package com.movrit.movrit.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.entity.ContentChannel;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 제출 전 점검 기준을 본인 신청·콘텐츠 준비 단계에서만 읽는지 확인합니다. */
class ContentPrecheckCriteriaReaderTest {
  private final CampaignApplicationRepository applications =
      mock(CampaignApplicationRepository.class);
  private final ContentPrecheckCriteriaReader reader =
      new ContentPrecheckCriteriaReader(applications);

  private CampaignApplication application(CampaignApplicationStatus status) {
    Campaign campaign = mock(Campaign.class);
    when(campaign.getTitle()).thenReturn("시카 앰플 체험단");
    when(campaign.getCampaignType()).thenReturn(CampaignType.PRODUCT);
    when(campaign.getContentChannel()).thenReturn(ContentChannel.BLOG);
    when(campaign.getMission()).thenReturn("2주 사용");
    when(campaign.getSubmissionRequirement()).thenReturn("사진 3장");
    when(campaign.getCaution()).thenReturn("협찬 표시");
    CampaignApplication application = mock(CampaignApplication.class);
    when(application.getStatus()).thenReturn(status);
    when(application.getCampaign()).thenReturn(campaign);
    return application;
  }

  @Test
  void activeAndRevisionRequestedApplicationsAreRead() {
    for (CampaignApplicationStatus status :
        new CampaignApplicationStatus[] {
          CampaignApplicationStatus.ACTIVE, CampaignApplicationStatus.REVISION_REQUESTED
        }) {
      CampaignApplication application = application(status);
      when(applications.findByIdAndCreatorMemberId(11L, 7L)).thenReturn(Optional.of(application));

      var criteria = reader.read(7L, 11L);

      assertThat(criteria.campaignType()).isEqualTo("PRODUCT");
      assertThat(criteria.submissionRequirement()).isEqualTo("사진 3장");
    }
  }

  @Test
  void otherMembersApplicationIs404() {
    when(applications.findByIdAndCreatorMemberId(11L, 8L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reader.read(8L, 11L))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void submittedOrCompletedApplicationIs409() {
    for (CampaignApplicationStatus status :
        new CampaignApplicationStatus[] {
          CampaignApplicationStatus.SUBMITTED,
          CampaignApplicationStatus.CONTENT_SUBMITTED,
          CampaignApplicationStatus.COMPLETED
        }) {
      CampaignApplication application = application(status);
      when(applications.findByIdAndCreatorMemberId(11L, 7L)).thenReturn(Optional.of(application));

      assertThatThrownBy(() -> reader.read(7L, 11L))
          .isInstanceOfSatisfying(
              ResponseStatusException.class,
              e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
  }
}
