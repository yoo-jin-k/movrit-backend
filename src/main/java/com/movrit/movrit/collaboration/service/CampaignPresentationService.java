package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.collaboration.dto.CampaignPresentationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignDetailImage;
import com.movrit.movrit.collaboration.repository.CampaignDetailImageRepository;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignPresentationService {
    private final CampaignRepository campaignRepository;
    private final CampaignDetailImageRepository detailImageRepository;
    private final ProductService productService;
    private final CampaignImageStorageService fileStorageService;

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
    public CampaignPresentationDtos.Response uploadDetail(Long memberId, Long campaignId, MultipartFile file, String altText) {
        Campaign campaign = owned(memberId, campaignId);
        String description = normalize(altText);
        if (description != null && description.codePointCount(0, description.length()) > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 설명은 200자 이하여야 합니다.");
        }
        long count = detailImageRepository.countByCampaignId(campaignId);
        if (count >= 5) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상세 이미지는 최대 5장까지 등록할 수 있습니다.");
        String url = fileStorageService.storeImage(file, "campaigns/" + campaignId + "/detail");
        detailImageRepository.save(CampaignDetailImage.builder().campaign(campaign).imageUrl(url)
                .displayOrder((int) count).altText(description).build());
        if (count == 0) campaign.updateDetailImage(url);
        return response(campaign);
    }

    @Transactional
    public CampaignPresentationDtos.Response reorderDetails(Long memberId, Long campaignId,
                                                              CampaignPresentationDtos.ReorderRequest request) {
        Campaign campaign = owned(memberId, campaignId);
        List<CampaignDetailImage> images = detailImageRepository.findAllByCampaignIdOrderByDisplayOrderAsc(campaignId);
        List<Long> ids = request.detailImageIds();
        if (ids == null || ids.size() != images.size() || ids.stream().distinct().count() != ids.size()
                || !images.stream().map(CampaignDetailImage::getId).collect(java.util.stream.Collectors.toSet())
                .equals(new java.util.HashSet<>(ids))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 상세 이미지 전체를 중복 없이 순서대로 보내 주세요.");
        }
        for (CampaignDetailImage image : images) image.reorder(-(image.getDisplayOrder() + 1));
        detailImageRepository.flush();
        for (int i = 0; i < ids.size(); i++) {
            final Long id = ids.get(i);
            final int order = i;
            images.stream().filter(image -> image.getId().equals(id)).findFirst().orElseThrow().reorder(order);
        }
        campaign.updateDetailImage(ids.isEmpty() ? null : images.stream().filter(i -> i.getId().equals(ids.get(0))).findFirst().orElseThrow().getImageUrl());
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
        detailImageRepository.deleteAll(detailImageRepository.findAllByCampaignIdOrderByDisplayOrderAsc(campaignId));
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
        List<CampaignPresentationDtos.DetailImage> details = detailImageRepository
                .findAllByCampaignIdOrderByDisplayOrderAsc(campaign.getId()).stream()
                .map(image -> new CampaignPresentationDtos.DetailImage(image.getId(), image.getImageUrl(),
                        image.getDisplayOrder(), image.getAltText())).toList();
        return new CampaignPresentationDtos.Response(campaign.getId(), campaign.getRepresentativeImageUrl(),
                campaign.getDetailImageUrl(), details, campaign.getVisitPlaceName(), campaign.getVisitAddress());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
