package com.movrit.movrit.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 캠페인이 프리오더 없이 브랜드에 직접 속해 만들어지는지, 프리오더와 브랜드가 어긋나면 거절하는지 확인합니다. */
class CollaborationCreateCampaignTest {

  private final CampaignRepository campaigns = mock(CampaignRepository.class);
  private final PreorderService preorders = mock(PreorderService.class);
  private final ProductService products = mock(ProductService.class);
  private final CampaignOwnershipService ownership = mock(CampaignOwnershipService.class);
  private final CollaborationService service =
      new CollaborationService(
          campaigns,
          mock(CollaborationRepository.class),
          mock(ReferralClickRepository.class),
          mock(OrderRepository.class),
          mock(OrderItemRepository.class),
          mock(CreatorService.class),
          preorders,
          products,
          mock(CampaignApplicationRepository.class),
          ownership);

  private final Brand brand = Brand.builder().id(5L).name("체험 브랜드").build();

  private CollaborationDtos.CampaignRequest request(Long preorderId, Long brandId) {
    return new CollaborationDtos.CampaignRequest(
        preorderId,
        brandId,
        "브랜드 단독 체험단",
        "설명",
        "뷰티",
        0L,
        5,
        null,
        "체험 제품 1개",
        LocalDateTime.now().minusHours(1),
        LocalDateTime.now().plusDays(7),
        null,
        null,
        null,
        null);
  }

  @Test
  void campaignWithoutPreorderBelongsToResolvedBrand() {
    when(ownership.resolveBrandForNewCampaign(1L, null)).thenReturn(brand);
    when(campaigns.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CollaborationDtos.CampaignResponse response = service.createCampaign(1L, request(null, null));

    ArgumentCaptor<Campaign> saved = ArgumentCaptor.forClass(Campaign.class);
    verify(campaigns).save(saved.capture());
    assertThat(saved.getValue().getBrand()).isSameAs(brand);
    assertThat(saved.getValue().getPreorder()).isNull();
    assertThat(response.preorderId()).isNull();
    assertThat(response.brandName()).isEqualTo("체험 브랜드");
    // 프리오더 경로를 거치지 않습니다.
    verify(preorders, never()).getPreorder(any());
  }

  @Test
  void preorderFromAnotherBrandIsRejected() {
    Product product = Product.builder().id(9L).brand(brand).build();
    Preorder preorder = Preorder.builder().id(30L).product(product).build();
    when(preorders.getPreorder(30L)).thenReturn(preorder);
    when(products.getOwnedProduct(1L, 9L)).thenReturn(product);

    assertThatThrownBy(() -> service.createCampaign(1L, request(30L, 777L)))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    verify(campaigns, never()).save(any());
  }

  @Test
  void localCampaignKeepsRegionAndRejectsUnknownRegion() {
    when(ownership.resolveBrandForNewCampaign(1L, null)).thenReturn(brand);
    when(campaigns.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));
    CollaborationDtos.CampaignRequest base = request(null, null);
    CollaborationDtos.CampaignRequest local =
        new CollaborationDtos.CampaignRequest(
            null,
            null,
            base.title(),
            base.description(),
            "맛집",
            0L,
            5,
            null,
            base.rewardDescription(),
            base.startAt(),
            base.endAt(),
            com.movrit.movrit.collaboration.entity.CampaignType.LOCAL,
            "서울",
            com.movrit.movrit.campaigncontent.entity.ContentChannel.BLOG,
            null);

    CollaborationDtos.CampaignResponse response = service.createCampaign(1L, local);

    assertThat(response.campaignType())
        .isEqualTo(com.movrit.movrit.collaboration.entity.CampaignType.LOCAL);
    assertThat(response.region()).isEqualTo("서울");
    assertThat(response.premier()).isFalse();
    // 목록에 없는 지역 이름은 400으로 거절합니다.
    CollaborationDtos.CampaignRequest unknown =
        new CollaborationDtos.CampaignRequest(
            null,
            null,
            base.title(),
            null,
            null,
            0L,
            5,
            null,
            null,
            base.startAt(),
            base.endAt(),
            com.movrit.movrit.collaboration.entity.CampaignType.LOCAL,
            "뉴욕",
            null,
            null);
    assertThatThrownBy(() -> service.createCampaign(1L, unknown))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void detailsAreSavedWithNewCampaignAndScheduleOrderIsChecked() {
    when(ownership.resolveBrandForNewCampaign(1L, null)).thenReturn(brand);
    when(campaigns.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));
    CollaborationDtos.CampaignRequest base = request(null, null);
    LocalDateTime selection = base.endAt().plusDays(1);
    CollaborationDtos.CampaignRequest withDetails =
        withDetails(
            base,
            new CollaborationDtos.CampaignDetails(
                "  제품을 2주간 사용하고 후기를 남겨 주세요.  ",
                "",
                null,
                "선정 후 3일 안에 발송합니다.",
                null,
                null,
                "#MOVRIT 해시태그 필수",
                selection,
                selection.plusDays(1),
                selection.plusDays(14),
                selection.plusDays(1),
                selection.plusDays(20),
                selection.plusDays(25)));

    service.createCampaign(1L, withDetails);

    ArgumentCaptor<Campaign> saved = ArgumentCaptor.forClass(Campaign.class);
    verify(campaigns).save(saved.capture());
    // 앞뒤 공백은 지우고, 빈 칸은 저장하지 않습니다.
    assertThat(saved.getValue().getMission()).isEqualTo("제품을 2주간 사용하고 후기를 남겨 주세요.");
    assertThat(saved.getValue().getChannelGuide()).isNull();
    assertThat(saved.getValue().getSelectionAnnouncedAt()).isEqualTo(selection);
    assertThat(saved.getValue().getResultsAnnouncedAt()).isEqualTo(selection.plusDays(25));

    // 선정 발표가 모집 종료보다 빠르면 400으로 거절합니다.
    CollaborationDtos.CampaignRequest wrongOrder =
        withDetails(
            base,
            new CollaborationDtos.CampaignDetails(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                base.endAt().minusDays(1),
                null,
                null,
                null,
                null,
                null));
    assertThatThrownBy(() -> service.createCampaign(1L, wrongOrder))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  private static CollaborationDtos.CampaignRequest withDetails(
      CollaborationDtos.CampaignRequest base, CollaborationDtos.CampaignDetails details) {
    return new CollaborationDtos.CampaignRequest(
        base.preorderId(),
        base.brandId(),
        base.title(),
        base.description(),
        base.category(),
        base.minFollowerCount(),
        base.recruitmentCount(),
        base.selectionMode(),
        base.rewardDescription(),
        base.startAt(),
        base.endAt(),
        base.campaignType(),
        base.region(),
        base.contentChannel(),
        details);
  }
}
