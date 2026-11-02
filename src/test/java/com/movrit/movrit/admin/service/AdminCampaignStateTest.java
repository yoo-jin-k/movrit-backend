package com.movrit.movrit.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.movrit.movrit.collaboration.entity.Campaign;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 운영자 캠페인 관리의 표시 상태(모집 예정·모집 중·모집 마감·숨김)와 숨김·다시 공개를 확인합니다. */
class AdminCampaignStateTest {
  private final LocalDateTime now = LocalDateTime.of(2026, 11, 2, 12, 0);

  private Campaign campaign(LocalDateTime start, LocalDateTime end) {
    return Campaign.builder().title("c").startAt(start).endAt(end).open(true).build();
  }

  @Test
  void stateFollowsPeriodAndOpenFlag() {
    assertThat(AdminCampaignService.state(campaign(now.plusDays(1), now.plusDays(5)), now))
        .isEqualTo("UPCOMING");
    assertThat(AdminCampaignService.state(campaign(now.minusDays(1), now.plusDays(5)), now))
        .isEqualTo("RECRUITING");
    assertThat(AdminCampaignService.state(campaign(now.minusDays(5), now.minusDays(1)), now))
        .isEqualTo("ENDED");

    Campaign hidden = campaign(now.minusDays(1), now.plusDays(5));
    hidden.close();
    assertThat(AdminCampaignService.state(hidden, now)).isEqualTo("HIDDEN");
    hidden.reopen();
    assertThat(AdminCampaignService.state(hidden, now)).isEqualTo("RECRUITING");
  }
}
