package com.movrit.movrit.collaboration.dto;

import com.movrit.movrit.collaboration.entity.Campaign;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class CampaignManagementDtos {
    private CampaignManagementDtos() {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 10000) String description,
            @Size(max = 100) String category,
            @NotNull @Min(0) Long minFollowerCount,
            @NotNull @Min(1) Integer recruitmentCount,
            @Size(max = 5000) String rewardDescription,
            @Size(max = 10000) String mission,
            @Size(max = 5000) String channelGuide,
            @Size(max = 5000) String scheduleGuide,
            @Size(max = 5000) String deliveryGuide,
            @Size(max = 5000) String visitGuide,
            @Size(max = 5000) String caution,
            @Size(max = 5000) String submissionRequirement,
            @NotNull Long expectedScheduleVersion,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt,
            LocalDateTime selectionAnnouncedAt,
            LocalDateTime experienceStartAt,
            LocalDateTime experienceEndAt,
            LocalDateTime contentSubmissionStartAt,
            LocalDateTime contentSubmissionEndAt,
            LocalDateTime resultsAnnouncedAt
    ) {
    }

    public record DetailResponse(
            Long id,
            Long preorderId,
            String preorderTitle,
            Long productId,
            String productName,
            String brandName,
            String title,
            String description,
            String category,
            Long minFollowerCount,
            Integer recruitmentCount,
            String rewardDescription,
            String mission,
            String channelGuide,
            String scheduleGuide,
            String deliveryGuide,
            String visitGuide,
            String representativeImageUrl,
            String detailImageUrl,
            String visitPlaceName,
            String visitAddress,
            String caution,
            String submissionRequirement,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime selectionAnnouncedAt,
            LocalDateTime experienceStartAt,
            LocalDateTime experienceEndAt,
            LocalDateTime contentSubmissionStartAt,
            LocalDateTime contentSubmissionEndAt,
            LocalDateTime resultsAnnouncedAt,
            Long scheduleVersion,
            boolean open
    ) {
        public static DetailResponse from(Campaign campaign) { return from(campaign, true); }

        /** 공개 조회에서는 배송 전용 캠페인의 방문 주소·안내를 제외합니다. */
        public static DetailResponse from(Campaign campaign, boolean includeVisit) {
            return new DetailResponse(
                    campaign.getId(),
                    campaign.getPreorder().getId(),
                    campaign.getPreorder().getTitle(),
                    campaign.getPreorder().getProduct().getId(),
                    campaign.getPreorder().getProduct().getName(),
                    campaign.getPreorder().getProduct().getBrand().getName(),
                    campaign.getTitle(),
                    campaign.getDescription(),
                    campaign.getCategory(),
                    campaign.getMinFollowerCount(),
                    campaign.getRecruitmentCount(),
                    campaign.getRewardDescription(),
                    campaign.getMission(),
                    campaign.getChannelGuide(),
                    campaign.getScheduleGuide(),
                    campaign.getDeliveryGuide(),
                    includeVisit ? campaign.getVisitGuide() : null,
                    campaign.getRepresentativeImageUrl(),
                    campaign.getDetailImageUrl(),
                    includeVisit ? campaign.getVisitPlaceName() : null,
                    includeVisit ? campaign.getVisitAddress() : null,
                    campaign.getCaution(),
                    campaign.getSubmissionRequirement(),
                    campaign.getStartAt(),
                    campaign.getEndAt(),
                    campaign.getSelectionAnnouncedAt(),
                    campaign.getExperienceStartAt(),
                    campaign.getExperienceEndAt(),
                    campaign.getContentSubmissionStartAt(),
                    campaign.getContentSubmissionEndAt(),
                    campaign.getResultsAnnouncedAt(),
                    campaign.getScheduleVersion(),
                    campaign.isOpen()
            );
        }
    }
}
