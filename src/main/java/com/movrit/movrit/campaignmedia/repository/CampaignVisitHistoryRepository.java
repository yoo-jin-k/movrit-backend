package com.movrit.movrit.campaignmedia.repository;
import com.movrit.movrit.campaignmedia.entity.CampaignVisitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CampaignVisitHistoryRepository extends JpaRepository<CampaignVisitHistory, Long> {
    List<CampaignVisitHistory> findAllByCampaignIdOrderByIdDesc(Long campaignId);
}
