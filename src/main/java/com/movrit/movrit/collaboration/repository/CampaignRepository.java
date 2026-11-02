package com.movrit.movrit.collaboration.repository;

import com.movrit.movrit.campaigncontent.entity.ContentChannel;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 캠페인(모집 공고)을 저장하고 조회합니다. */
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
  /** 프리오더에 연결된 캠페인을 찾습니다. 프리오더 하나에는 캠페인이 하나만 연결됩니다. */
  Optional<Campaign> findByPreorderId(Long id);

  /** 선정 인원 계산과 상태 변경을 같은 잠금 안에서 처리해 정원 초과를 막습니다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select campaign from Campaign campaign where campaign.id = :id")
  Optional<Campaign> findByIdForUpdate(@Param("id") Long id);

  /** 공개(open) 상태인 캠페인을 최신순으로 조회합니다. 모집 기간 필터는 서비스에서 적용합니다. */
  List<Campaign> findAllByOpenTrueOrderByIdDesc();

  /**
   * 캠페인 목록 검색입니다. 모집이 시작된 공개 캠페인 중 조건에 맞는 것을 최신순으로 돌려줍니다. 비어 있는(null) 조건은 거르지 않습니다.
   *
   * <p>검색어는 제목·소개·제공 내역·브랜드 이름에서 찾고(%·_는 글자 그대로), 모집 중/마감 순서와 인기·선정 확률 정렬은 신청 수가 필요해 서비스에서 합니다.
   */
  @Query(
      "select c from Campaign c join fetch c.brand b where c.open = true and c.startAt <= :now"
          + " and (:keyword is null or lower(c.title) like :keyword escape '\\'"
          + "      or lower(coalesce(c.description, '')) like :keyword escape '\\'"
          + "      or lower(coalesce(c.rewardDescription, '')) like :keyword escape '\\'"
          + "      or lower(b.name) like :keyword escape '\\')"
          + " and (:type is null or c.campaignType = :type)"
          + " and (:category is null or c.category = :category)"
          + " and (:region is null or c.region = :region)"
          + " and (:channel is null or c.contentChannel = :channel)"
          + " and (:premier = false or c.premier = true)"
          // 날짜 null 비교는 PostgreSQL이 타입을 정하지 못하므로, '오늘 오픈'이 아니면 아주 이른 날짜를 넘깁니다.
          + " and c.startAt >= :openedFrom"
          + " order by c.id desc")
  List<Campaign> searchVisible(
      @Param("now") LocalDateTime now,
      @Param("keyword") String keyword,
      @Param("type") CampaignType type,
      @Param("category") String category,
      @Param("region") String region,
      @Param("channel") ContentChannel channel,
      @Param("premier") boolean premier,
      @Param("openedFrom") LocalDateTime openedFrom);

  /** 해당 회원이 소유한 브랜드의 캠페인을 최신순으로 조회합니다(업체 화면용). */
  List<Campaign> findAllByBrandSellerMemberIdOrderByIdDesc(Long id);

  /** 업체 회원이 운영 중(공개)인 캠페인 수입니다(탈퇴 가능 여부 확인용). */
  long countByBrandSellerMemberIdAndOpenTrue(Long memberId);
}
