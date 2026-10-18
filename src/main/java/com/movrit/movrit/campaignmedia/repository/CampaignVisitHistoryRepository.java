package com.movrit.movrit.campaignmedia.repository;

import com.movrit.movrit.campaignmedia.entity.CampaignVisitHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignVisitHistoryRepository extends JpaRepository<CampaignVisitHistory, Long> {
  List<CampaignVisitHistory> findAllByCampaignIdOrderByIdDesc(Long campaignId);
}
