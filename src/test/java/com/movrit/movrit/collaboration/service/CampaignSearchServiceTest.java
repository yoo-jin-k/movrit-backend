package com.movrit.movrit.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository.CampaignApplicationCount;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignType;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** 캠페인 목록 검색의 정렬·페이지·입력 규칙을 확인합니다. */
class CampaignSearchServiceTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 12, 0);

  private final CampaignRepository campaigns = mock(CampaignRepository.class);
  private final CampaignApplicationRepository applications =
      mock(CampaignApplicationRepository.class);
  private final CampaignSearchService service = new CampaignSearchService(campaigns, applications);

  private static Campaign campaign(long id, int recruit, LocalDateTime endAt) {
    return Campaign.builder()
        .id(id)
        .title("캠페인 " + id)
        .brand(Brand.builder().id(1L).name("루미").build())
        .recruitmentCount(recruit)
        .startAt(NOW.minusDays(5))
        .endAt(endAt)
        .open(true)
        .build();
  }

  private static CampaignApplicationCount count(long campaignId, long value) {
    return new CampaignApplicationCount() {
      @Override
      public Long getCampaignId() {
        return campaignId;
      }

      @Override
      public Long getApplicationCount() {
        return value;
      }
    };
  }

  private CampaignSearchService.Query query(String sort, int page, int size) {
    return new CampaignSearchService.Query(
        null, null, null, null, null, false, false, sort, page, size);
  }

  private void given(List<Campaign> found, List<CampaignApplicationCount> counts) {
    when(campaigns.searchVisible(any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
        .thenReturn(found);
    when(applications.countActiveApplicationsByCampaignIds(any())).thenReturn(counts);
  }

  @Test
  void endedCampaignsGoLastAndSortsApplyWithinGroup() {
    Campaign ended = campaign(9, 10, NOW.minusDays(1));
    Campaign soon = campaign(3, 10, NOW.plusDays(1));
    Campaign later = campaign(5, 2, NOW.plusDays(7));
    given(List.of(ended, later, soon), List.of(count(3, 1), count(5, 6), count(9, 30)));

    assertThat(ids(service.search(query("latest", 0, 20), NOW))).containsExactly(5L, 3L, 9L);
    assertThat(ids(service.search(query("closing", 0, 20), NOW))).containsExactly(3L, 5L, 9L);
    assertThat(ids(service.search(query("popular", 0, 20), NOW))).containsExactly(5L, 3L, 9L);
    // 경쟁률: 3번 1/10, 5번 6/2 → 3번이 먼저입니다.
    assertThat(ids(service.search(query("chance", 0, 20), NOW))).containsExactly(3L, 5L, 9L);
  }

  @Test
  void pagesAndCountsAreReturned() {
    given(
        List.of(
            campaign(4, 5, NOW.plusDays(3)),
            campaign(3, 5, NOW.plusDays(3)),
            campaign(2, 5, NOW.plusDays(3))),
        List.of(count(2, 1)));
    CollaborationDtos.CampaignPage second = service.search(query("latest", 1, 2), NOW);
    assertThat(second.total()).isEqualTo(3);
    assertThat(second.totalPages()).isEqualTo(2);
    assertThat(ids(second)).containsExactly(2L);
    assertThat(second.items().get(0).applicationCount()).isEqualTo(1L);
    assertThat(service.search(query("latest", 0, 999), NOW).size())
        .isEqualTo(CampaignSearchService.MAX_SIZE);
  }

  @Test
  void filtersAreConvertedAndValidated() {
    when(campaigns.searchVisible(
            eq(NOW),
            eq("%100\\%%"),
            eq(CampaignType.LOCAL),
            isNull(),
            eq("서울"),
            isNull(),
            eq(false),
            eq(NOW.toLocalDate().atStartOfDay())))
        .thenReturn(List.of());
    var result =
        service.search(
            new CampaignSearchService.Query(
                " 100% ", "local", "", "서울", null, false, true, "latest", 0, 20),
            NOW);
    assertThat(result.total()).isZero();

    assertThatThrownBy(() -> service.search(query("random", 0, 20), NOW))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                service.search(
                    new CampaignSearchService.Query(
                        null, "SHOP", null, null, null, false, false, "latest", 0, 20),
                    NOW))
        .hasMessageContaining("캠페인 종류");
  }

  private static List<Long> ids(CollaborationDtos.CampaignPage page) {
    return page.items().stream().map(CollaborationDtos.CampaignResponse::id).toList();
  }
}
