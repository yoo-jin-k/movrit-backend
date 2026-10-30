package com.movrit.movrit.campaignfavorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignfavorite.dto.CampaignFavoriteDtos;
import com.movrit.movrit.campaignfavorite.entity.CampaignFavorite;
import com.movrit.movrit.campaignfavorite.repository.CampaignFavoriteRepository;
import com.movrit.movrit.campaignfavorite.service.CampaignFavoriteService;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.member.service.MemberService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** 캠페인 찜의 추가·해제·목록 규칙을 확인합니다. */
class CampaignFavoriteServiceTest {
  private final CampaignFavoriteRepository favorites = mock(CampaignFavoriteRepository.class);
  private final CampaignRepository campaigns = mock(CampaignRepository.class);
  private final CampaignApplicationRepository applications =
      mock(CampaignApplicationRepository.class);
  private final CampaignFavoriteService service =
      new CampaignFavoriteService(favorites, campaigns, applications, mock(MemberService.class));

  @Test
  void addSavesOnceThroughConflictSafeInsert() {
    when(campaigns.findById(1L)).thenReturn(Optional.of(campaign(1L, false)));

    service.add(7L, 1L);

    // 중복 여부는 DB의 on conflict 처리에 맡기므로 항상 같은 저장 쿼리를 호출합니다.
    verify(favorites).insertIfAbsent(7L, 1L);
  }

  @Test
  void hiddenOrMissingCampaignCannotBeFavorited() {
    when(campaigns.findById(2L)).thenReturn(Optional.of(campaign(2L, true)));
    when(campaigns.findById(3L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.add(7L, 2L)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.add(7L, 3L)).isInstanceOf(ResponseStatusException.class);
    verify(favorites, never()).insertIfAbsent(anyLong(), anyLong());
  }

  @Test
  void removeIgnoresCampaignThatWasNotFavorited() {
    when(favorites.findByMemberIdAndCampaignId(7L, 4L)).thenReturn(Optional.empty());

    service.remove(7L, 4L);

    verify(favorites, never()).delete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void listHidesCampaignsOfDeletedBrandAndAttachesApplicationCount() {
    CampaignFavorite visible = favorite(campaign(5L, false));
    CampaignFavorite hidden = favorite(campaign(6L, true));
    when(favorites.findAllByMemberIdOrderByIdDesc(7L)).thenReturn(List.of(visible, hidden));
    when(applications.countActiveApplicationsByCampaignIds(anyCollection())).thenReturn(List.of());

    List<CampaignFavoriteDtos.Item> result = service.findMine(7L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).campaign().id()).isEqualTo(5L);
    assertThat(result.get(0).campaign().applicationCount()).isZero();
  }

  /** brandDeleted가 true면 운영 브랜드가 삭제된(공개 대상이 아닌) 캠페인을 만듭니다. */
  private static Campaign campaign(Long id, boolean brandDeleted) {
    Brand brand =
        Brand.builder()
            .id(99L)
            .name("테스트 브랜드")
            .deletedAt(brandDeleted ? LocalDateTime.now() : null)
            .build();
    return Campaign.builder()
        .id(id)
        .brand(brand)
        .title("캠페인 " + id)
        .recruitmentCount(10)
        .open(true)
        .startAt(LocalDateTime.now().minusDays(1))
        .endAt(LocalDateTime.now().plusDays(5))
        .build();
  }

  private static CampaignFavorite favorite(Campaign campaign) {
    return CampaignFavorite.builder().campaign(campaign).build();
  }
}
