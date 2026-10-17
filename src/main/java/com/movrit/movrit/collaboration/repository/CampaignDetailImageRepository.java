package com.movrit.movrit.collaboration.repository;

import com.movrit.movrit.collaboration.entity.CampaignDetailImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CampaignDetailImageRepository extends JpaRepository<CampaignDetailImage, Long> {
    List<CampaignDetailImage> findAllByCampaignIdOrderByDisplayOrderAsc(Long campaignId);
    long countByCampaignId(Long campaignId);
}
