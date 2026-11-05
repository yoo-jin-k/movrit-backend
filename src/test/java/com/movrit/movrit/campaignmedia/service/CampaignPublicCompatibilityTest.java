package com.movrit.movrit.campaignmedia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignManagementService;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.product.entity.Product;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CampaignPublicCompatibilityTest {
  @Test
  void closedDeliveryCampaignBodyHidesVisitFieldsButPreservesStoredLegacyData() {
    var repository = mock(CampaignRepository.class);
    var forms = mock(CampaignApplicationFormRepository.class);
    var product = Product.builder().id(20L).brand(Brand.builder().name("업체").build()).build();
    var campaign =
        Campaign.builder()
            .brand(Brand.builder().id(99L).name("테스트 브랜드").build())
            .id(10L)
            .open(false)
            .preorder(Preorder.builder().id(30L).product(product).build())
            .visitGuide("이전 방문 안내")
            .visitPlaceName("이전 장소")
            .visitAddress("이전 주소")
            .build();
    when(repository.findById(10L)).thenReturn(Optional.of(campaign));
    when(forms.findByCampaignId(10L))
        .thenReturn(
            Optional.of(
                CampaignApplicationForm.builder()
                    .allowedFulfillmentTypesJson("[\"DELIVERY\"]")
                    .build()));
    var service =
        new CampaignManagementService(
            repository,
            mock(CampaignApplicationRepository.class),
            forms,
            mock(CampaignOwnershipService.class),
            mock(AuditLogService.class),
            mock(com.movrit.movrit.member.repository.MemberRepository.class));
    var response = service.getDetail(10L);
    assertNull(response.visitGuide());
    assertNull(response.visitPlaceName());
    assertNull(response.visitAddress());
    assertEquals("이전 주소", campaign.getVisitAddress());
    assertTrue(CampaignPublicPolicy.visible(campaign));
  }

  @Test
  void adminHiddenCampaignDetailIsOnlyForAdminOwnerAndApplicants() {
    var repository = mock(CampaignRepository.class);
    var applications = mock(CampaignApplicationRepository.class);
    var members = mock(com.movrit.movrit.member.repository.MemberRepository.class);
    var campaign =
        Campaign.builder()
            .brand(Brand.builder().id(99L).name("브랜드").build())
            .id(11L)
            .open(true)
            .build();
    campaign.hideByAdmin();
    when(repository.findById(11L)).thenReturn(Optional.of(campaign));
    when(applications.existsByCampaignIdAndCreatorMemberId(11L, 5L)).thenReturn(true);
    when(members.findById(7L))
        .thenReturn(
            Optional.of(
                com.movrit.movrit.member.entity.Member.builder()
                    .role(com.movrit.movrit.member.entity.MemberRole.ADMIN)
                    .build()));
    var service =
        new CampaignManagementService(
            repository,
            applications,
            mock(CampaignApplicationFormRepository.class),
            mock(CampaignOwnershipService.class),
            mock(AuditLogService.class),
            members);

    // 비로그인·관계없는 회원은 존재를 알 수 없게 404입니다.
    var guest =
        assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> service.getDetailFor(11L, null));
    assertEquals(404, guest.getStatusCode().value());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.getDetailFor(11L, 3L));
    // 신청한 회원과 운영자는 볼 수 있습니다.
    assertNotNull(service.getDetailFor(11L, 5L));
    assertNotNull(service.getDetailFor(11L, 7L));

    // 다시 공개하면 누구나 볼 수 있습니다.
    campaign.reopen();
    assertFalse(campaign.isAdminHidden());
    assertNotNull(service.getDetailFor(11L, null));
  }
}
