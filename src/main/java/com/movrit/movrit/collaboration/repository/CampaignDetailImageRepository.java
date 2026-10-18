package com.movrit.movrit.collaboration.repository;

import com.movrit.movrit.collaboration.entity.CampaignDetailImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignDetailImageRepository extends JpaRepository<CampaignDetailImage, Long> {
  List<CampaignDetailImage> findAllByCampaignIdOrderByDisplayOrderAsc(Long campaignId);

  long countByCampaignId(Long campaignId);
}
