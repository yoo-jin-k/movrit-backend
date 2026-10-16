package com.movrit.movrit.campaigncontent.repository;

import com.movrit.movrit.campaigncontent.entity.CampaignContentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignContentSubmissionRepository extends JpaRepository<CampaignContentSubmission, Long> {
    Optional<CampaignContentSubmission> findByApplicationId(Long applicationId);

    List<CampaignContentSubmission> findAllByApplicationCreatorMemberIdOrderByIdDesc(Long memberId);

    List<CampaignContentSubmission> findAllByApplicationCampaignPreorderProductBrandSellerMemberIdOrderByIdDesc(Long memberId);
}
