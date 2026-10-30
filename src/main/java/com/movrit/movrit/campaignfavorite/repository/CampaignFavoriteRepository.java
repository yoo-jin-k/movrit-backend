package com.movrit.movrit.campaignfavorite.repository;

import com.movrit.movrit.campaignfavorite.entity.CampaignFavorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 캠페인 찜을 조회합니다. 항상 회원 ID를 조건으로 넣어 본인 찜만 다룹니다. */
public interface CampaignFavoriteRepository extends JpaRepository<CampaignFavorite, Long> {
  /** 찜 목록 화면용으로 캠페인·브랜드·프리오더를 함께 불러와 최근 찜한 순으로 정렬합니다. */
  @EntityGraph(attributePaths = {"campaign", "campaign.brand", "campaign.preorder.product"})
  List<CampaignFavorite> findAllByMemberIdOrderByIdDesc(Long memberId);

  /** 카드의 하트 표시에 쓰는 찜한 캠페인 ID 목록입니다. 캠페인 본문은 불러오지 않습니다. */
  @Query("select f.campaign.id from CampaignFavorite f where f.member.id = :memberId")
  List<Long> findCampaignIdsByMemberId(@Param("memberId") Long memberId);

  /** 회원이 특정 캠페인을 찜한 기록을 찾습니다. 찜 해제에 사용합니다. */
  Optional<CampaignFavorite> findByMemberIdAndCampaignId(Long memberId, Long campaignId);

  /**
   * 찜을 저장합니다. 이미 같은 찜이 있으면(동시 요청 포함) 아무것도 하지 않습니다.
   *
   * @return 새로 저장했으면 1, 이미 있었으면 0입니다.
   */
  @Modifying
  @Query(
      value =
          "insert into campaign_favorite (member_id, campaign_id, created_at, updated_at)"
              + " values (:memberId, :campaignId, now(), now())"
              + " on conflict (member_id, campaign_id) do nothing",
      nativeQuery = true)
  int insertIfAbsent(@Param("memberId") Long memberId, @Param("campaignId") Long campaignId);
}
