package com.movrit.movrit.campaigncontent.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignContentSubmissionTest {

    @Test
    void revisionRequestedContentCanBeResubmitted() {
        CampaignContentSubmission submission = CampaignContentSubmission.builder()
                .status(ContentSubmissionStatus.REVISION_REQUESTED)
                .revisionCount(0)
                .reviewComment("필수 문구를 추가해 주세요.")
                .build();

        submission.resubmit("https://example.com/revised", "수정했습니다.");

        assertThat(submission.getStatus()).isEqualTo(ContentSubmissionStatus.SUBMITTED);
        assertThat(submission.getRevisionCount()).isEqualTo(1);
        assertThat(submission.getReviewComment()).isNull();
        assertThat(submission.getSubmittedAt()).isNotNull();
    }

    @Test
    void approvedContentCannotReceiveAnotherRevisionRequest() {
        CampaignContentSubmission submission = CampaignContentSubmission.builder()
                .status(ContentSubmissionStatus.APPROVED)
                .revisionCount(0)
                .build();

        assertThatThrownBy(() -> submission.requestRevision("다시 수정해 주세요."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("검토 대기 중인 콘텐츠에만 수정을 요청할 수 있습니다.");
    }

    @Test
    void brandApprovalWaitsForOperatorApproval() {
        CampaignContentSubmission submission = CampaignContentSubmission.builder()
                .status(ContentSubmissionStatus.SUBMITTED)
                .revisionCount(0)
                .build();

        submission.approveByBrand("브랜드 검토를 마쳤습니다.");

        assertThat(submission.getStatus()).isEqualTo(ContentSubmissionStatus.BRAND_APPROVED);
        submission.approveByOperator("최종 승인했습니다.");
        assertThat(submission.getStatus()).isEqualTo(ContentSubmissionStatus.APPROVED);
    }
}
