package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.collaboration.dto.CampaignPresentationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.common.file.LocalFileStorageService;
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
    private final ProductService products = mock(ProductService.class);
    private final LocalFileStorageService files = mock(LocalFileStorageService.class);
    private final CampaignPresentationService service = new CampaignPresentationService(campaigns, products, files);

    @Test
    void ownerCanSavePublicVisitLocationWithoutPersonalContact() {
        Campaign campaign = campaign();
        when(campaigns.findById(10L)).thenReturn(Optional.of(campaign));

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
        when(campaigns.findById(10L)).thenReturn(Optional.of(campaign));
        when(files.storeImage(image, "campaigns/10/representative"))
                .thenReturn("/uploads/campaigns/10/representative/safe.png");

        var result = service.uploadRepresentative(1L, 10L, image);

        assertThat(result.representativeImageUrl()).isEqualTo("/uploads/campaigns/10/representative/safe.png");
    }

    private Campaign campaign() {
        Product product = Product.builder().id(20L).build();
        Preorder preorder = Preorder.builder().id(30L).product(product).build();
        return Campaign.builder().id(10L).preorder(preorder).build();
    }
}
