package com.movrit.movrit.campaignfavorite.service;

import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignfavorite.dto.CampaignFavoriteDtos;
import com.movrit.movrit.campaignfavorite.entity.CampaignFavorite;
import com.movrit.movrit.campaignfavorite.repository.CampaignFavoriteRepository;
import com.movrit.movrit.campaignmedia.service.CampaignPublicPolicy;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.member.service.MemberService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 캠페인 찜(관심 캠페인 저장) 기능입니다.
 *
 * <p>찜 추가·해제는 여러 번 눌러도 결과가 같도록(이미 찜했으면 그대로, 이미 해제했으면 그대로) 처리합니다. 공개 대상이 아닌 캠페인(브랜드 삭제 등)은 새로 찜할 수
 * 없고 목록에서도 보이지 않습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignFavoriteService {
  private final CampaignFavoriteRepository favorites;
  private final CampaignRepository campaigns;
  private final CampaignApplicationRepository campaignApplications;
  private final MemberService memberService;

  /**
   * 내가 찜한 캠페인 목록을 최근 찜한 순으로 돌려줍니다.
   *
   * <p>카드에 "신청 N / 모집 M명"을 보여줄 수 있도록 신청 수를 한 번에 집계해 붙입니다.
   */
  public List<CampaignFavoriteDtos.Item> findMine(Long memberId) {
    List<CampaignFavorite> mine =
        favorites.findAllByMemberIdOrderByIdDesc(memberId).stream()
            .filter(f -> CampaignPublicPolicy.visible(f.getCampaign()))
            .toList();
    if (mine.isEmpty()) return List.of();
    Map<Long, Long> counts = new HashMap<>();
    campaignApplications
        .countActiveApplicationsByCampaignIds(
            mine.stream().map(f -> f.getCampaign().getId()).toList())
        .forEach(row -> counts.put(row.getCampaignId(), row.getApplicationCount()));
    return mine.stream()
        .map(
            f ->
                new CampaignFavoriteDtos.Item(
                    f.getCreatedAt(),
                    CollaborationDtos.CampaignResponse.from(
                        f.getCampaign(), counts.getOrDefault(f.getCampaign().getId(), 0L))))
        .toList();
  }

  /** 카드의 하트를 칠하기 위해 내가 찜한 캠페인 ID만 돌려줍니다. */
  public List<Long> findMyCampaignIds(Long memberId) {
    return favorites.findCampaignIdsByMemberId(memberId);
  }

  /**
   * 캠페인을 찜합니다. 이미 찜한 캠페인이면 아무것도 바꾸지 않습니다.
   *
   * <p>없는 캠페인이나 공개 대상이 아닌 캠페인은 404로 응답합니다. 두 요청이 동시에 들어와도 DB가 중복 저장을 막아(on conflict do nothing) 한
   * 건만 남습니다.
   */
  @Transactional
  public void add(Long memberId, Long campaignId) {
    Campaign campaign =
        campaigns
            .findById(campaignId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    CampaignPublicPolicy.requireVisible(campaign);
    // 탈퇴 등으로 없는 회원이면 여기서 막습니다.
    memberService.getMember(memberId);
    favorites.insertIfAbsent(memberId, campaignId);
  }

  /** 찜을 해제합니다. 찜하지 않은 캠페인이면 아무것도 바꾸지 않습니다. */
  @Transactional
  public void remove(Long memberId, Long campaignId) {
    favorites.findByMemberIdAndCampaignId(memberId, campaignId).ifPresent(favorites::delete);
  }
}
