package com.movrit.movrit.campaignapplication.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CampaignApplicationTest {

  @Test
  void submittedApplicationCanBeSelectedAndActivated() {
    CampaignApplication application = application(CampaignApplicationStatus.SUBMITTED);

    application.select();
    application.activate();

    assertThat(application.getStatus()).isEqualTo(CampaignApplicationStatus.ACTIVE);
    assertThat(application.getRespondedAt()).isNotNull();
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
  void submittedApplicationCanBeCancelledWithoutChangingPaymentData() {
    CampaignApplication application = application(CampaignApplicationStatus.SUBMITTED);

    application.cancel("개인 일정으로 참여가 어렵습니다.");

    assertThat(application.getStatus()).isEqualTo(CampaignApplicationStatus.CANCELLED);
    assertThat(application.getCancellationReason()).isEqualTo("개인 일정으로 참여가 어렵습니다.");
    assertThat(application.getCancelledAt()).isNotNull();
  }

  @Test
  void participantCannotCancelAfterSelection() {
    // 선정·참여 중·완료 상태에서는 본인 취소를 막고 업체 문의를 안내합니다.
    for (CampaignApplicationStatus status :
        List.of(
            CampaignApplicationStatus.SELECTED,
            CampaignApplicationStatus.ACTIVE,
            CampaignApplicationStatus.COMPLETED)) {
      CampaignApplication application = application(status);
      assertThatThrownBy(() -> application.cancel("선정 후 취소"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("선정 전 접수 상태에서만");
    }
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
