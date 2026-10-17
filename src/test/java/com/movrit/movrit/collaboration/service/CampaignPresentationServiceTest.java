package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.collaboration.dto.CampaignPresentationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.repository.CampaignDetailImageRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignPresentationServiceTest {
    private final CampaignRepository campaigns = mock(CampaignRepository.class);
    private final CampaignDetailImageRepository detailImages = mock(CampaignDetailImageRepository.class);
    private final ProductService products = mock(ProductService.class);
    private final CampaignImageStorageService files = mock(CampaignImageStorageService.class);
    private final CampaignPresentationService service = new CampaignPresentationService(campaigns, detailImages, products, files, mock(com.movrit.movrit.campaignmedia.repository.CampaignMediaStateRepository.class), mock(com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository.class), mock(com.movrit.movrit.campaignmedia.service.CampaignMediaService.class));

    @Test
    void ownerCanSavePublicVisitLocationWithoutPersonalContact() {
        Campaign campaign = campaign();
        when(campaigns.findByIdForUpdate(10L)).thenReturn(Optional.of(campaign));

        CampaignPresentationDtos.Response result = service.saveLocation(1L, 10L,
                new CampaignPresentationDtos.SaveRequest("모브릿 체험관", "서울특별시 강남구 테헤란로 1"));

        assertThat(result.visitPlaceName()).isEqualTo("모브릿 체험관");
        assertThat(result.visitAddress()).isEqualTo("서울특별시 강남구 테헤란로 1");
        verify(products).getOwnedProduct(1L, 20L);
    }

    @Test
    void representativeImageUsesCampaignSpecificDirectory() {
        Campaign campaign = campaign();
        var image = new MockMultipartFile("file", "campaign.png", "image/png", new byte[]{1});
        when(campaigns.findByIdForUpdate(10L)).thenReturn(Optional.of(campaign));
        when(files.storeImage(image, "campaigns/10/representative"))
                .thenReturn("/uploads/campaigns/10/representative/safe.png");

        var result = service.uploadRepresentative(1L, 10L, image);

        assertThat(result.representativeImageUrl()).isEqualTo("/uploads/campaigns/10/representative/safe.png");
    }

    @Test
    void detailImageIsAddedInDisplayOrder() {
        Campaign campaign = campaign();
        var image = new MockMultipartFile("file", "detail.png", "image/png", new byte[]{1});
        when(campaigns.findByIdForUpdate(10L)).thenReturn(Optional.of(campaign));
        when(detailImages.countByCampaignId(10L)).thenReturn(2L);
        when(files.storeImage(image, "campaigns/10/detail")).thenReturn("/uploads/detail.png");

        service.uploadDetail(1L, 10L, image, "사용 모습");

        verify(detailImages).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getDisplayOrder() == 2 && "사용 모습".equals(saved.getAltText())));
    }

    @Test
    void rejectsLongDescriptionBeforeWritingFileOrDatabase() {
        Campaign campaign = campaign();
        when(campaigns.findByIdForUpdate(10L)).thenReturn(Optional.of(campaign));
        var file = new MockMultipartFile("file", "detail.png", "image/png", new byte[]{1});
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.uploadDetail(1L, 10L, file, "가".repeat(201)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        org.mockito.Mockito.verifyNoInteractions(files);
        org.mockito.Mockito.verify(detailImages, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsDescriptionAtTwoHundredCharacterBoundary() {
        Campaign campaign = campaign();
        when(campaigns.findByIdForUpdate(10L)).thenReturn(Optional.of(campaign));
        var file = new MockMultipartFile("file", "detail.png", "image/png", new byte[]{1});
        when(files.storeImage(file, "campaigns/10/detail")).thenReturn("/uploads/detail.png");
        service.uploadDetail(1L, 10L, file, "가".repeat(200));
        verify(detailImages).save(org.mockito.ArgumentMatchers.argThat(saved -> saved.getAltText().length() == 200));
    }

    private Campaign campaign() {
        when(detailImages.findAllByCampaignIdOrderByDisplayOrderAsc(10L)).thenReturn(java.util.List.of());
        Product product = Product.builder().id(20L).build();
        Preorder preorder = Preorder.builder().id(30L).product(product).build();
        return Campaign.builder().id(10L).preorder(preorder).build();
    }
}
