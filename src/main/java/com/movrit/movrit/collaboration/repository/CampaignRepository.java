package com.movrit.movrit.collaboration.repository;

import com.movrit.movrit.collaboration.entity.Campaign;
import jakarta.persistence.LockModeType;
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

  /** 해당 회원이 소유한 브랜드의 캠페인을 최신순으로 조회합니다(업체 화면용). */
  List<Campaign> findAllByBrandSellerMemberIdOrderByIdDesc(Long id);
}
