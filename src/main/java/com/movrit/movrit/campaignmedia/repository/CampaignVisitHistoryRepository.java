package com.movrit.movrit.campaignmedia.repository;

import com.movrit.movrit.campaignmedia.entity.CampaignVisitHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 방문 장소 변경 기록을 저장하고 최신순으로 조회합니다. */
public interface CampaignVisitHistoryRepository extends JpaRepository<CampaignVisitHistory, Long> {
  /** 캠페인 방문 장소 변경 이력을 최근 순으로 조회합니다. */
  List<CampaignVisitHistory> findAllByCampaignIdOrderByIdDesc(Long campaignId);
}
