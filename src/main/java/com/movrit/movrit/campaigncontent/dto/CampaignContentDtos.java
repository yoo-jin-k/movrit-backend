package com.movrit.movrit.campaigncontent.dto;

import com.movrit.movrit.campaigncontent.entity.ContentSubmissionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class CampaignContentDtos {
    private CampaignContentDtos() {
    }

    public record SubmitRequest(
            @NotBlank @Size(max = 1000) String contentUrl,
            @Size(max = 2000) String note
    ) {
    }

    public record ReviewRequest(@NotBlank @Size(max = 2000) String comment) {
    }

    public record ApproveRequest(@Size(max = 2000) String comment) {
    }

    public record Response(
            Long id,
            Long applicationId,
            String creatorNickname,
            String campaignTitle,
            String contentUrl,
            String note,
            ContentSubmissionStatus status,
            Integer revisionCount,
            String reviewComment,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt
    ) {
    }
}
