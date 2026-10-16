package com.movrit.movrit.collaboration.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignTest {

    @Test
    void updateStoresCampaignSpecificGuides() {
        Campaign campaign = Campaign.builder().open(true).build();
        LocalDateTime startAt = LocalDateTime.now().plusDays(1);
        LocalDateTime endAt = startAt.plusDays(7);

        campaign.update("가을 신상품 체험단", "상세 소개", "뷰티", 1000L, 10,
                "제품 제공", "사진 5장", "인스타그램", "선정 후 7일 이내",
                "택배 발송", null, "과장 표현 금지", "게시물 URL 제출", startAt, endAt);

        assertThat(campaign.getMission()).isEqualTo("사진 5장");
        assertThat(campaign.getChannelGuide()).isEqualTo("인스타그램");
        assertThat(campaign.getSubmissionRequirement()).isEqualTo("게시물 URL 제출");
    }

    @Test
    void updateRejectsInvalidRecruitmentPeriod() {
        Campaign campaign = Campaign.builder().open(true).build();
        LocalDateTime startAt = LocalDateTime.now().plusDays(7);

        assertThatThrownBy(() -> campaign.update("캠페인", null, null, 0L, 1,
                null, null, null, null, null, null, null, null, startAt, startAt.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("모집 종료일은 시작일보다 뒤여야 합니다.");
    }
}
