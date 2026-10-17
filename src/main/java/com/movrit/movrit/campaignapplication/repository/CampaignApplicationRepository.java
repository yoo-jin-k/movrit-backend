package com.movrit.movrit.campaignapplication.repository;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CampaignApplicationRepository extends JpaRepository<CampaignApplication, Long> {
    boolean existsByCampaignIdAndCreatorId(Long campaignId, Long creatorId);
    boolean existsByCampaignIdAndCreatorMemberId(Long campaignId, Long memberId);

    boolean existsByCampaignId(Long campaignId);

    Optional<CampaignApplication> findByIdAndCreatorMemberId(Long id, Long memberId);
    List<CampaignApplication> findAllByCreatorMemberIdOrderByIdDesc(Long memberId);
    List<CampaignApplication> findAllByCampaignPreorderProductBrandSellerMemberIdOrderByIdDesc(Long memberId);
    long countByCampaignIdAndStatusIn(Long campaignId, List<CampaignApplicationStatus> statuses);
}
