package com.movrit.movrit.creator.dto;

import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.entity.CreatorCampaign;
import com.movrit.movrit.creator.entity.CreatorCampaignStatus;
import com.movrit.movrit.creator.entity.CreatorStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CreatorDtos {
    private CreatorDtos() {}

    public record ApplyRequest(
            @NotBlank @Size(max = 100) String nickname,
            @Size(max = 2000) String bio,
            @Size(max = 100) String primaryCategory,
            @NotBlank @Size(max = 30) String channelPlatform,
            @NotBlank @Size(max = 100) String channelHandle,
            @NotBlank @Size(max = 500) String channelUrl,
            @NotNull @Min(0) Long followerCount
    ) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String nickname,
            @Size(max = 2000) String bio,
            @Size(max = 100) String primaryCategory,
            @NotBlank @Size(max = 30) String channelPlatform,
            @NotBlank @Size(max = 100) String channelHandle,
            @NotBlank @Size(max = 500) String channelUrl,
            @NotNull @Min(0) Long followerCount
    ) {}

    public record RejectRequest(
            @NotBlank @Size(max = 500) String reason
    ) {}

    public record AssignCampaignRequest(
            @NotNull Long preorderId,
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal commissionRate
    ) {}

    public record CampaignStatusRequest(
            @NotNull CreatorCampaignStatus status
    ) {}

    public record Response(
            Long id,
            Long memberId,
            String nickname,
            String bio,
            String primaryCategory,
            String channelPlatform,
            String channelHandle,
            String channelUrl,
            Long followerCount,
            CreatorStatus status,
            String rejectionReason
    ) {
        public static Response from(Creator creator) {
            return new Response(
                    creator.getId(), creator.getMember().getId(), creator.getNickname(), creator.getBio(),
                    creator.getPrimaryCategory(), creator.getChannelPlatform(), creator.getChannelHandle(),
                    creator.getChannelUrl(), creator.getFollowerCount(), creator.getStatus(), creator.getRejectionReason()
            );
        }
    }

    public record CampaignResponse(
            Long campaignId,
            Long preorderId,
            String preorderTitle,
            CreatorCampaignStatus status,
            BigDecimal commissionRate,
            Long orderCount,
            BigDecimal grossSales,
            BigDecimal estimatedEarnings,
            String currency
    ) {
        public static CampaignResponse from(CreatorCampaign campaign) {
            long orderCount = campaign.getPreorder().getOrderedQuantity() == null
                    ? 0L : campaign.getPreorder().getOrderedQuantity();
            BigDecimal grossSales = campaign.getPreorder().getPreorderPrice()
                    .multiply(BigDecimal.valueOf(orderCount));
            BigDecimal estimatedEarnings = grossSales
                    .multiply(campaign.getCommissionRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            return new CampaignResponse(
                    campaign.getId(),
                    campaign.getPreorder().getId(),
                    campaign.getPreorder().getTitle(),
                    campaign.getStatus(),
                    campaign.getCommissionRate(),
                    orderCount,
                    grossSales,
                    estimatedEarnings,
                    campaign.getPreorder().getCurrency()
            );
        }
    }

    public record DashboardResponse(
            Response profile,
            long proposedCampaigns,
            long activeCampaigns,
            long completedCampaigns,
            long totalOrders,
            BigDecimal grossSales,
            BigDecimal estimatedEarnings,
            List<CampaignResponse> campaigns
    ) {}
}
