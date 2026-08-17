package com.movrit.movrit.creator.repository;

import com.movrit.movrit.creator.entity.CreatorCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreatorCampaignRepository extends JpaRepository<CreatorCampaign, Long> {
    List<CreatorCampaign> findAllByCreatorIdOrderByCreatedAtDesc(Long creatorId);
    Optional<CreatorCampaign> findByCreatorIdAndPreorderId(Long creatorId, Long preorderId);
    boolean existsByCreatorIdAndPreorderId(Long creatorId, Long preorderId);
}
