package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.CampaignExtensionRequest;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 기간 연장 요청 저장소입니다(신청 ID가 기본 키). */
public interface CampaignExtensionRequestRepository
    extends JpaRepository<CampaignExtensionRequest, Long> {

  boolean existsByApplicationIdAndStatus(Long applicationId, String status);

  /** 업체가 운영하는 캠페인의 연장 요청입니다. 확인 중인 요청을 먼저, 최근 순으로 보여 줍니다. */
  @Query(
      "select e from CampaignExtensionRequest e, CampaignApplication a"
          + " where a.id = e.applicationId and a.campaign.brand.seller.member.id = :sellerMemberId"
          + " order by case when e.status = 'REQUESTED' then 0 else 1 end, e.createdAt desc")
  List<CampaignExtensionRequest> findAllForSeller(@Param("sellerMemberId") Long sellerMemberId);

  /** 운영자용 전체 연장 요청입니다. 확인 중인 요청을 먼저, 최근 순으로 보여 줍니다. */
  @Query(
      "select e from CampaignExtensionRequest e"
          + " order by case when e.status = 'REQUESTED' then 0 else 1 end, e.createdAt desc")
  List<CampaignExtensionRequest> findAllForAdmin(Pageable page);

  long countByMemberIdAndStatus(Long memberId, String status);
}
