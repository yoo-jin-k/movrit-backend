package com.movrit.movrit.collaboration.repository;

import com.movrit.movrit.collaboration.entity.Campaign;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Optional<Campaign> findByPreorderId(Long id);

    /** 선정 인원 계산과 상태 변경을 같은 잠금 안에서 처리해 정원 초과를 막습니다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select campaign from Campaign campaign where campaign.id = :id")
    Optional<Campaign> findByIdForUpdate(@Param("id") Long id);

    List<Campaign> findAllByOpenTrueOrderByIdDesc();

    List<Campaign> findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(Long id);
}
