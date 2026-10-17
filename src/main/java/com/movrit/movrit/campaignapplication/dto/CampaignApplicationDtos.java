package com.movrit.movrit.campaignapplication.dto;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class CampaignApplicationDtos {
    private CampaignApplicationDtos() {}

    public enum QuestionType {
        TEXT,
        TEXTAREA,
        SINGLE_CHOICE
    }

    public record QuestionDefinition(
            @NotBlank String id,
            @NotBlank String label,
            boolean required,
            @NotNull QuestionType type,
            @NotNull List<@NotBlank String> options,
            @Positive Integer maxLength
    ) {
        public QuestionDefinition {
            type = type == null ? QuestionType.TEXT : type;
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

    public record AgreementDefinition(
            @NotBlank String id,
            @NotBlank String title,
            @NotBlank String version,
            boolean required,
            @NotBlank @Size(max = 20000) String body
    ) {}

    public record FormSaveRequest(
            @NotEmpty List<@NotNull FulfillmentType> allowedFulfillmentTypes,
            @NotNull List<@Valid QuestionDefinition> questions,
            @NotNull List<@Valid AgreementDefinition> agreements
    ) {}

    public record FormResponse(
            Long campaignId,
            Long version,
            List<FulfillmentType> allowedFulfillmentTypes,
            List<QuestionDefinition> questions,
            List<AgreementDefinition> agreements
    ) {}

    public record ShippingContact(
            String recipientName,
            String recipientPhone,
            String postalCode,
            String address1,
            String address2
    ) {}

    public record Answer(@NotBlank String questionId, String value) {}

    public record AgreementAcceptance(
            @NotBlank String agreementId,
            @NotBlank String version,
            boolean accepted
    ) {}

    public record CreateRequest(
            @NotBlank String message,
            @NotNull Long expectedFormVersion,
            Long expectedEligibilityRequirementId,
            Long expectedEligibilityRequirementVersion,
            @NotNull FulfillmentType fulfillmentType,
            @Valid ShippingContact shippingContact,
            @NotNull List<@Valid Answer> answers,
            @NotNull List<@Valid AgreementAcceptance> agreementAcceptances
    ) {}

    public record CancelRequest(@NotBlank String reason) {}

    public record EligibilityResponse(
            Long campaignId,
            Long formVersion,
            String applicationEligibilityPolicy,
            boolean emailVerified,
            boolean identityVerified,
            boolean channelReady,
            boolean channelProfileExists,
            CampaignEligibilityDtos.RequirementResponse campaignRequirement,
            CampaignEligibilityDtos.MetricEvaluation metricEvaluation,
            boolean alreadyApplied,
            boolean canApply,
            List<String> blockingReasons
    ) {}

    public record Response(
            Long id,
            Long campaignId,
            Long preorderId,
            Long creatorId,
            String creatorNickname,
            FulfillmentType fulfillmentType,
            CampaignApplicationStatus status,
            String message,
            ShippingContact shippingContact,
            List<Answer> answers,
            List<AgreementAcceptance> agreementAcceptances,
            Long formVersion,
            CampaignEligibilityDtos.RequirementResponse eligibilityRequirementSnapshot,
            Long eligibilityRequirementVersion,
            boolean identityVerificationConfigured,
            boolean identityVerified,
            boolean emailVerified,
            boolean channelReady,
            List<String> readinessNotices,
            LocalDateTime submittedAt,
            LocalDateTime respondedAt,
            LocalDateTime cancelledAt,
            String cancellationReason,
            LocalDateTime experienceConfirmedAt
    ) {}
}
