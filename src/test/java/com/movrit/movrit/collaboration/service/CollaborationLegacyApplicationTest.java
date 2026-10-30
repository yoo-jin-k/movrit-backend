package com.movrit.movrit.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.repository.CollaborationRepository;
import com.movrit.movrit.collaboration.repository.ReferralClickRepository;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.service.CreatorService;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.order.repository.OrderItemRepository;
import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.product.service.ProductService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CollaborationLegacyApplicationTest {

  @Test
  void unverifiedEmailCannotUseLegacyCampaignApplication() {
    CampaignRepository campaignRepository = mock(CampaignRepository.class);
    CollaborationRepository collaborationRepository = mock(CollaborationRepository.class);
    CreatorService creatorService = mock(CreatorService.class);
    Member member = Member.builder().build();
    Creator creator =
        Creator.builder()
            .member(member)
            .status("ACTIVE")
            .followerCount(1_000L)
            .snsUrl("https://example.com/channel")
            .build();
    Campaign campaign =
        Campaign.builder()
            .open(true)
            .startAt(LocalDateTime.now().minusDays(1))
            .endAt(LocalDateTime.now().plusDays(1))
            .minFollowerCount(100L)
            .build();
    when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
    when(creatorService.getByMember(12L)).thenReturn(creator);
    CollaborationService service =
        new CollaborationService(
            campaignRepository,
            collaborationRepository,
            mock(ReferralClickRepository.class),
            mock(OrderRepository.class),
            mock(OrderItemRepository.class),
            creatorService,
            mock(PreorderService.class),
            mock(ProductService.class),
            mock(CampaignApplicationRepository.class));

    assertThatThrownBy(() -> service.apply(12L, 10L, new CollaborationDtos.ApplyRequest("신청")))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> {
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
              assertThat(exception.getReason()).isEqualTo("이메일 소유 확인 후 캠페인에 신청할 수 있습니다.");
            });
    verifyNoInteractions(collaborationRepository);
  }
}
