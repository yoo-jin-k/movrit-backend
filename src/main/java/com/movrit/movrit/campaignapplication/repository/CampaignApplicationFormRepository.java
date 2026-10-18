package com.movrit.movrit.campaignapplication.repository;

import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignApplicationFormRepository
    extends JpaRepository<CampaignApplicationForm, Long> {
  Optional<CampaignApplicationForm> findByCampaignId(Long campaignId);
}
