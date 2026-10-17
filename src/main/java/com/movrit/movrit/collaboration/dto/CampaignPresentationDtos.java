package com.movrit.movrit.collaboration.dto;

import jakarta.validation.constraints.Size;

public final class CampaignPresentationDtos {
    private CampaignPresentationDtos() {}

    public record SaveRequest(
            @Size(max = 200) String visitPlaceName,
            @Size(max = 500) String visitAddress
    ) {}

    public record Response(
            Long campaignId,
            String representativeImageUrl,
            String detailImageUrl,
            String visitPlaceName,
            String visitAddress
    ) {}
}
