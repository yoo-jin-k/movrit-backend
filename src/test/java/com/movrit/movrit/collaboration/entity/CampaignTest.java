package com.movrit.movrit.collaboration.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CampaignTest {

  @Test
  void updateStoresCampaignSpecificGuides() {
    Campaign campaign = Campaign.builder().open(true).build();
    LocalDateTime startAt = LocalDateTime.now().plusDays(1);
    LocalDateTime endAt = startAt.plusDays(7);

    campaign.update(
        "가을 신상품 체험단",
        "상세 소개",
        "뷰티",
        1000L,
        10,
        "제품 제공",
        "사진 5장",
        "인스타그램",
        "선정 후 7일 이내",
        "택배 발송",
        null,
        "과장 표현 금지",
        "게시물 URL 제출",
        1L,
        startAt,
        endAt,
        null,
        null,
        null,
        null,
        null,
        null);

    assertThat(campaign.getMission()).isEqualTo("사진 5장");
    assertThat(campaign.getChannelGuide()).isEqualTo("인스타그램");
    assertThat(campaign.getSubmissionRequirement()).isEqualTo("게시물 URL 제출");
  }

  @Test
  void updateRejectsInvalidRecruitmentPeriod() {
    Campaign campaign = Campaign.builder().open(true).build();
    LocalDateTime startAt = LocalDateTime.now().plusDays(7);

    assertThatThrownBy(
            () ->
                campaign.update(
                    "캠페인",
                    null,
                    null,
                    0L,
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1L,
                    startAt,
                    startAt.minusDays(1),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("모집 종료일은 시작일보다 뒤여야 합니다.");
  }

  @Test
  void updateStoresEachScheduleWithoutDerivingMissingDates() {
    Campaign campaign = Campaign.builder().open(true).scheduleVersion(1L).build();
    LocalDateTime recruitmentStart = LocalDateTime.of(2026, 10, 12, 0, 0);
    LocalDateTime recruitmentEnd = LocalDateTime.of(2026, 10, 21, 0, 0);
    LocalDateTime selectionAt = LocalDateTime.of(2026, 10, 22, 12, 0);
    LocalDateTime experienceStart = LocalDateTime.of(2026, 10, 23, 0, 0);
    LocalDateTime experienceEnd = LocalDateTime.of(2026, 10, 30, 0, 0);
    LocalDateTime submissionStart = LocalDateTime.of(2026, 10, 24, 0, 0);
    LocalDateTime submissionEnd = LocalDateTime.of(2026, 11, 3, 0, 0);

    campaign.update(
        "캠페인",
        null,
        null,
        0L,
        1,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        1L,
        recruitmentStart,
        recruitmentEnd,
        selectionAt,
        experienceStart,
        experienceEnd,
        submissionStart,
        submissionEnd,
        null);

    assertThat(campaign.getSelectionAnnouncedAt()).isEqualTo(selectionAt);
    assertThat(campaign.getContentSubmissionStartAt()).isEqualTo(submissionStart);
    assertThat(campaign.getResultsAnnouncedAt()).isNull();
    assertThat(campaign.getScheduleVersion()).isEqualTo(2L);
  }
}
