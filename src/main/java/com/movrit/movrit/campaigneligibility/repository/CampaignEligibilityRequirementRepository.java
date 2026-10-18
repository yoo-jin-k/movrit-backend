package com.movrit.movrit.campaigneligibility.repository;

import com.movrit.movrit.campaigneligibility.entity.CampaignEligibilityRequirement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignEligibilityRequirementRepository
    extends JpaRepository<CampaignEligibilityRequirement, Long> {
  Optional<CampaignEligibilityRequirement> findByCampaignId(Long campaignId);

  void deleteByCampaignId(Long campaignId);
}
