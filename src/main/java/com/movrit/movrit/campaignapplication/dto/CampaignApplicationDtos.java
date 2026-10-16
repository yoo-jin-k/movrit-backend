package com.movrit.movrit.campaignapplication.dto;

import com.movrit.movrit.campaignapplication.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class CampaignApplicationDtos {
    private CampaignApplicationDtos() {}

    public record QuestionDefinition(
            @NotBlank String id,
            @NotBlank String label,
            boolean required
    ) {}

    public record AgreementDefinition(
            @NotBlank String id,
            @NotBlank String title,
            @NotBlank String version,
            boolean required
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
            @NotNull FulfillmentType fulfillmentType,
            @Valid ShippingContact shippingContact,
            @NotNull List<@Valid Answer> answers,
            @NotNull List<@Valid AgreementAcceptance> agreementAcceptances
    ) {}

    public record CancelRequest(@NotBlank String reason) {}

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
            boolean identityVerificationConfigured,
            boolean identityVerified,
            boolean channelReady,
            List<String> readinessNotices,
            LocalDateTime submittedAt,
            LocalDateTime respondedAt,
            LocalDateTime cancelledAt,
            String cancellationReason,
            LocalDateTime experienceConfirmedAt
    ) {}
}
