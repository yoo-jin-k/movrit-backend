package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.collaboration.dto.CampaignPresentationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.common.file.LocalFileStorageService;
import com.movrit.movrit.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignPresentationService {
    private final CampaignRepository campaignRepository;
    private final ProductService productService;
    private final LocalFileStorageService fileStorageService;

    public CampaignPresentationDtos.Response find(Long campaignId) {
        return response(get(campaignId));
    }

    @Transactional
    public CampaignPresentationDtos.Response saveLocation(
            Long memberId, Long campaignId, CampaignPresentationDtos.SaveRequest request
    ) {
        Campaign campaign = owned(memberId, campaignId);
        campaign.updatePresentation(normalize(request.visitPlaceName()), normalize(request.visitAddress()));
        return response(campaign);
    }

    @Transactional
    public CampaignPresentationDtos.Response uploadRepresentative(Long memberId, Long campaignId, MultipartFile file) {
        Campaign campaign = owned(memberId, campaignId);
        campaign.updateRepresentativeImage(fileStorageService.storeImage(file,
                "campaigns/" + campaignId + "/representative"));
        return response(campaign);
    }

    @Transactional
    public CampaignPresentationDtos.Response uploadDetail(Long memberId, Long campaignId, MultipartFile file) {
        Campaign campaign = owned(memberId, campaignId);
        campaign.updateDetailImage(fileStorageService.storeImage(file,
                "campaigns/" + campaignId + "/detail"));
        return response(campaign);
    }

    @Transactional
    public CampaignPresentationDtos.Response clearRepresentative(Long memberId, Long campaignId) {
        Campaign campaign = owned(memberId, campaignId);
        campaign.updateRepresentativeImage(null);
        return response(campaign);
    }

    @Transactional
    public CampaignPresentationDtos.Response clearDetail(Long memberId, Long campaignId) {
        Campaign campaign = owned(memberId, campaignId);
        campaign.updateDetailImage(null);
        return response(campaign);
    }

    private Campaign owned(Long memberId, Long campaignId) {
        Campaign campaign = get(campaignId);
        productService.getOwnedProduct(memberId, campaign.getPreorder().getProduct().getId());
        return campaign;
    }

    private Campaign get(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }

    private CampaignPresentationDtos.Response response(Campaign campaign) {
        return new CampaignPresentationDtos.Response(campaign.getId(), campaign.getRepresentativeImageUrl(),
                campaign.getDetailImageUrl(), campaign.getVisitPlaceName(), campaign.getVisitAddress());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
