package com.movrit.movrit.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.seller.dto.AdminSellerDtos;
import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.entity.SellerStatus;
import com.movrit.movrit.seller.repository.SellerProfileRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 운영자 업체 목록의 권한·정렬·상태 거르기를 확인합니다. */
class AdminSellerQueryServiceTest {

  private final SellerProfileRepository repository = mock(SellerProfileRepository.class);
  private final MemberService memberService = mock(MemberService.class);
  private final AdminSellerQueryService service =
      new AdminSellerQueryService(repository, memberService);

  @Test
  void defaultSortIsNewestApplicationFirst() {
    when(repository.findAll(any(Sort.class))).thenReturn(List.of());

    service.findForAdmin(1L, null, null);

    ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
    verify(repository).findAll(sort.capture());
    assertThat(sort.getValue().getOrderFor("createdAt").getDirection())
        .isEqualTo(Sort.Direction.DESC);
    assertThat(sort.getValue().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void oldestAndNameSortsMapToAscendingOrders() {
    assertThat(AdminSellerQueryService.sortOf("oldest").getOrderFor("createdAt").isAscending())
        .isTrue();
    assertThat(AdminSellerQueryService.sortOf("name").getOrderFor("businessName").isAscending())
        .isTrue();
    assertThat(AdminSellerQueryService.sortOf(" ").getOrderFor("createdAt").isDescending())
        .isTrue();
  }

  @Test
  void unknownSortIsBadRequest() {
    assertThatThrownBy(() -> service.findForAdmin(1L, null, "createdAt;drop"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(error -> ((ResponseStatusException) error).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void statusFilterKeepsOnlyMatchingSellersInSortedOrder() {
    when(repository.findAll(any(Sort.class)))
        .thenReturn(
            List.of(
                seller(3L, SellerStatus.PENDING),
                seller(2L, SellerStatus.APPROVED),
                seller(1L, SellerStatus.PENDING)));

    List<AdminSellerDtos.ListItem> result =
        service.findForAdmin(1L, SellerStatus.PENDING, "newest");

    assertThat(result).extracting(AdminSellerDtos.ListItem::id).containsExactly(3L, 1L);
    assertThat(result).allMatch(item -> item.status() == SellerStatus.PENDING);
  }

  @Test
  void nonAdminIsRejectedBeforeReadingSellers() {
    when(memberService.getAdmin(2L))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."));

    assertThatThrownBy(() -> service.findForAdmin(2L, null, null))
        .isInstanceOf(ResponseStatusException.class);
    verify(repository, never()).findAll(any(Sort.class));
  }

  private static SellerProfile seller(Long id, SellerStatus status) {
    return SellerProfile.builder()
        .id(id)
        .member(Member.builder().id(100L + id).build())
        .businessName("업체 " + id)
        .status(status)
        .build();
  }
}
