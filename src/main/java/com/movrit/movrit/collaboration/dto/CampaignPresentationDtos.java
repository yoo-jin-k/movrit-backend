package com.movrit.movrit.collaboration.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

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
            List<DetailImage> detailImages,
            String visitPlaceName,
            String visitAddress
    ) {}

    public record DetailImage(Long id, String imageUrl, Integer displayOrder, String altText) {}

    public record ReorderRequest(@Size(max = 5) List<Long> detailImageIds) {}
}
