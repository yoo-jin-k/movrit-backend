package com.movrit.movrit.campaignapplication.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void activeApplicationCanBeCancelledWithoutChangingPaymentData() {
        CampaignApplication application = application(CampaignApplicationStatus.ACTIVE);

        application.cancel("개인 일정으로 참여가 어렵습니다.");

        assertThat(application.getStatus()).isEqualTo(CampaignApplicationStatus.CANCELLED);
        assertThat(application.getCancellationReason()).isEqualTo("개인 일정으로 참여가 어렵습니다.");
        assertThat(application.getCancelledAt()).isNotNull();
    }

    @Test
    void completedApplicationCannotBeCancelled() {
        CampaignApplication application = application(CampaignApplicationStatus.COMPLETED);

        assertThatThrownBy(() -> application.cancel("완료 후 취소"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("접수·선정·진행 상태에서만 참여를 취소할 수 있습니다.");
    }

    private CampaignApplication application(CampaignApplicationStatus status) {
        return CampaignApplication.builder()
                .status(status)
                .build();
    }
}
