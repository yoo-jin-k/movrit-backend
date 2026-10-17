package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignManagementService {
    private final CampaignRepository campaignRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final CampaignApplicationFormRepository formRepository;
    private final ProductService productService;
    private final AuditLogService auditLogService;

    public CampaignManagementDtos.DetailResponse getDetail(Long campaignId) {
        return CampaignManagementDtos.DetailResponse.from(getCampaign(campaignId));
    }

    @Transactional
    public CampaignManagementDtos.DetailResponse update(
            Long memberId,
            Long campaignId,
            CampaignManagementDtos.UpdateRequest request
    ) {
        Campaign campaign = getOwnedCampaign(memberId, campaignId);
        try {
            campaign.update(
                    request.title().trim(),
                    normalize(request.description()),
                    normalize(request.category()),
                    request.minFollowerCount(),
                    request.recruitmentCount(),
                    normalize(request.rewardDescription()),
                    normalize(request.mission()),
                    normalize(request.channelGuide()),
                    normalize(request.scheduleGuide()),
                    normalize(request.deliveryGuide()),
                    normalize(request.visitGuide()),
                    normalize(request.caution()),
                    normalize(request.submissionRequirement()),
                    request.startAt(),
                    request.endAt(),
                    request.selectionAnnouncedAt(),
                    request.experienceStartAt(),
                    request.experienceEndAt(),
                    request.contentSubmissionStartAt(),
                    request.contentSubmissionEndAt(),
                    request.resultsAnnouncedAt()
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        auditLogService.record(memberId, "CAMPAIGN_UPDATED", "CAMPAIGN", campaignId, null, null, null);
        return CampaignManagementDtos.DetailResponse.from(campaign);
    }

    @Transactional
    public CampaignManagementDtos.DetailResponse close(Long memberId, Long campaignId) {
        Campaign campaign = getOwnedCampaign(memberId, campaignId);
        campaign.close();
        auditLogService.record(memberId, "CAMPAIGN_CLOSED", "CAMPAIGN", campaignId,
                "{\"open\":true}", "{\"open\":false}", null);
        return CampaignManagementDtos.DetailResponse.from(campaign);
    }

    /** 신청 이력이 생긴 뒤에는 감사 추적을 위해 삭제하지 않고 종료만 허용합니다. */
    @Transactional
    public void delete(Long memberId, Long campaignId) {
        Campaign campaign = getOwnedCampaign(memberId, campaignId);
        if (applicationRepository.existsByCampaignId(campaignId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "신청 이력이 있는 캠페인은 삭제할 수 없습니다. 모집을 종료해 주세요.");
        }
        formRepository.findByCampaignId(campaignId).ifPresent(formRepository::delete);
        campaignRepository.delete(campaign);
        auditLogService.record(memberId, "CAMPAIGN_DELETED", "CAMPAIGN", campaignId, null, null, null);
    }

    private Campaign getOwnedCampaign(Long memberId, Long campaignId) {
        Campaign campaign = getCampaign(campaignId);
        productService.getOwnedProduct(memberId, campaign.getPreorder().getProduct().getId());
        return campaign;
    }

    private Campaign getCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
