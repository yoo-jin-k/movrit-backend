package com.movrit.movrit.campaignapplication.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CampaignApplicationTest {

  @Test
  void selectionStartsParticipationWithoutAcceptance() {
    // 선정되면 참여자의 별도 수락 없이 바로 진행 중이 됩니다.
    CampaignApplication application = application(CampaignApplicationStatus.SUBMITTED);

    application.select();

    assertThat(application.getStatus()).isEqualTo(CampaignApplicationStatus.ACTIVE);
    assertThat(application.getRespondedAt()).isNotNull();
  }

  @Test
  void legacySelectedApplicationCanStillBeActivated() {
    CampaignApplication application = application(CampaignApplicationStatus.SELECTED);

    application.activate();

    assertThat(application.getStatus()).isEqualTo(CampaignApplicationStatus.ACTIVE);
  }

  @Test
  void rejectedApplicationCannotBeActivated() {
    CampaignApplication application = application(CampaignApplicationStatus.SUBMITTED);
    application.reject();

    assertThatThrownBy(application::activate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("선정된 신청만 참여를 확정할 수 있습니다.");
  }

  @Test
  void contentCannotBeSubmittedBeforeExperienceConfirmation() {
    CampaignApplication application = application(CampaignApplicationStatus.ACTIVE);

    assertThatThrownBy(application::markContentSubmitted)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("체험 완료 확인 후 콘텐츠를 제출해 주세요.");
  }

  private CampaignApplication application(CampaignApplicationStatus status) {
    return CampaignApplication.builder().status(status).build();
  }
}
