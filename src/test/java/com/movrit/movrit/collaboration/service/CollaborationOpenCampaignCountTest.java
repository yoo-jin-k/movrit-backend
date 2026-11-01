package com.movrit.movrit.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository.CampaignApplicationCount;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.repository.CollaborationRepository;
import com.movrit.movrit.collaboration.repository.ReferralClickRepository;
import com.movrit.movrit.creator.service.CreatorService;
import com.movrit.movrit.order.repository.OrderItemRepository;
import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.service.ProductService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 공개 캠페인 목록이 카드 표시용 신청 수를 올바르게 붙이는지 확인합니다. */
class CollaborationOpenCampaignCountTest {

  private final CampaignRepository campaignRepository = mock(CampaignRepository.class);
  private final CampaignApplicationRepository applicationRepository =
      mock(CampaignApplicationRepository.class);
  private final CollaborationService service =
      new CollaborationService(
          campaignRepository,
          mock(CollaborationRepository.class),
          mock(ReferralClickRepository.class),
          mock(OrderRepository.class),
          mock(OrderItemRepository.class),
          mock(CreatorService.class),
          mock(PreorderService.class),
          mock(ProductService.class),
          applicationRepository,
          mock(CampaignOwnershipService.class));

  @Test
  void openCampaignsIncludeActiveApplicationCountAndZeroForNoApplications() {
    Campaign withApplications = campaign(1L, LocalDateTime.now().plusDays(3));
    Campaign withoutApplications = campaign(2L, LocalDateTime.now().plusDays(5));
    when(campaignRepository.findAllByOpenTrueOrderByIdDesc())
        .thenReturn(List.of(withApplications, withoutApplications));
    when(applicationRepository.countActiveApplicationsByCampaignIds(anyCollection()))
        .thenReturn(List.of(count(1L, 7L)));

    List<CollaborationDtos.CampaignResponse> result = service.openCampaigns();

    assertThat(result)
        .extracting(CollaborationDtos.CampaignResponse::applicationCount)
        .containsExactly(7L, 0L);
  }

  @Test
  void endedCampaignsStayVisibleAfterRecruitingOnes() {
    // 모집이 끝난 캠페인도 목록에서 사라지지 않고, 모집 중인 캠페인 뒤에 놓입니다.
    Campaign ended = campaign(3L, LocalDateTime.now().minusDays(1));
    Campaign recruiting = campaign(2L, LocalDateTime.now().plusDays(2));
    when(campaignRepository.findAllByOpenTrueOrderByIdDesc())
        .thenReturn(List.of(ended, recruiting));
    when(applicationRepository.countActiveApplicationsByCampaignIds(anyCollection()))
        .thenReturn(List.of());

    assertThat(service.openCampaigns())
        .extracting(CollaborationDtos.CampaignResponse::id)
        .containsExactly(2L, 3L);
  }

  @Test
  void emptyListSkipsCountQuery() {
    when(campaignRepository.findAllByOpenTrueOrderByIdDesc()).thenReturn(List.of());

    assertThat(service.openCampaigns()).isEmpty();
    // 보여줄 캠페인이 없으면 빈 IN 조건 쿼리를 보내지 않습니다.
    verify(applicationRepository, never()).countActiveApplicationsByCampaignIds(anyCollection());
  }

  private static Campaign campaign(Long id, LocalDateTime endAt) {
    Brand brand = Brand.builder().name("테스트 브랜드").build();
    Product product = Product.builder().brand(brand).build();
    Preorder preorder = Preorder.builder().id(id * 10).title("연결 프리오더").product(product).build();
    return Campaign.builder()
        .brand(Brand.builder().id(99L).name("테스트 브랜드").build())
        .id(id)
        .preorder(preorder)
        .title("캠페인 " + id)
        .recruitmentCount(10)
        .open(true)
        .startAt(LocalDateTime.now().minusDays(1))
        .endAt(endAt)
        .build();
  }

  private static CampaignApplicationCount count(Long campaignId, Long applicationCount) {
    return new CampaignApplicationCount() {
      @Override
      public Long getCampaignId() {
        return campaignId;
      }

      @Override
      public Long getApplicationCount() {
        return applicationCount;
      }
    };
  }
}
