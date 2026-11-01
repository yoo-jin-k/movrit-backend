package com.movrit.movrit.campaignapplication.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 업체·운영자의 선정 편집(선정 취소·미선정 되돌리기·참여 취소) 규칙을 확인합니다. */
class CampaignApplicationManageTest {

  private static CampaignApplication application(CampaignApplicationStatus status) {
    return CampaignApplication.builder().status(status).build();
  }

  @Test
  void selectionCanBeUndoneBeforeExperienceConfirmation() {
    CampaignApplication selected = application(CampaignApplicationStatus.SUBMITTED);
    selected.select();
    selected.unselect();
    assertThat(selected.getStatus()).isEqualTo(CampaignApplicationStatus.SUBMITTED);

    // 참여자가 체험 완료를 확인한 뒤에는 선정 취소가 아니라 '참여 취소'로 처리해야 합니다.
    CampaignApplication experienced = application(CampaignApplicationStatus.SUBMITTED);
    experienced.select();
    experienced.confirmExperience();
    assertThatThrownBy(experienced::unselect).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectedApplicationCanBeReviewedAgain() {
    CampaignApplication rejected = application(CampaignApplicationStatus.REJECTED);
    rejected.restoreToReview();
    assertThat(rejected.getStatus()).isEqualTo(CampaignApplicationStatus.SUBMITTED);
  }

  @Test
  void managerCancelKeepsReasonAndBlocksAfterContentSubmitted() {
    CampaignApplication active = application(CampaignApplicationStatus.ACTIVE);
    active.cancelByManager("참여자 개인 사정");
    assertThat(active.getStatus()).isEqualTo(CampaignApplicationStatus.CANCELLED);
    assertThat(active.getCancellationReason()).isEqualTo("[업체 취소] 참여자 개인 사정");
    assertThat(active.getCancelledAt()).isNotNull();

    assertThatThrownBy(
            () -> application(CampaignApplicationStatus.CONTENT_SUBMITTED).cancelByManager("사유"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> application(CampaignApplicationStatus.SUBMITTED).cancelByManager("사유"))
        .isInstanceOf(IllegalStateException.class);
  }
}
