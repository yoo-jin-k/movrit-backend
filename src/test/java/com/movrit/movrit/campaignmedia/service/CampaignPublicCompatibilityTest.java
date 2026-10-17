package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.repository.*;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignManagementService;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.product.service.ProductService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CampaignPublicCompatibilityTest {
    @Test void closedDeliveryCampaignBodyHidesVisitFieldsButPreservesStoredLegacyData() {
        var repository = mock(CampaignRepository.class);
        var forms = mock(CampaignApplicationFormRepository.class);
        var product = Product.builder().id(20L).brand(Brand.builder().name("업체").build()).build();
        var campaign = Campaign.builder().id(10L).open(false)
                .preorder(Preorder.builder().id(30L).product(product).build())
                .visitGuide("이전 방문 안내").visitPlaceName("이전 장소").visitAddress("이전 주소").build();
        when(repository.findById(10L)).thenReturn(Optional.of(campaign));
        when(forms.findByCampaignId(10L)).thenReturn(Optional.of(CampaignApplicationForm.builder()
                .allowedFulfillmentTypesJson("[\"DELIVERY\"]").build()));
        var service = new CampaignManagementService(repository, mock(CampaignApplicationRepository.class), forms,
                mock(ProductService.class), mock(AuditLogService.class));
        var response = service.getDetail(10L);
        assertNull(response.visitGuide()); assertNull(response.visitPlaceName()); assertNull(response.visitAddress());
        assertEquals("이전 주소", campaign.getVisitAddress());
        assertTrue(CampaignPublicPolicy.visible(campaign));
    }
}
