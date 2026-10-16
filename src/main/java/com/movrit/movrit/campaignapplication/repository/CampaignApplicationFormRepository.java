package com.movrit.movrit.campaignapplication.repository;

import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CampaignApplicationFormRepository extends JpaRepository<CampaignApplicationForm, Long> {
    Optional<CampaignApplicationForm> findByCampaignId(Long campaignId);
}
