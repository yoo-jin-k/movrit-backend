package com.movrit.movrit.campaignmedia.repository;

import com.movrit.movrit.campaignmedia.entity.CampaignVisitSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

/** 신청 당시 방문 정보 사본을 신청 ID로 저장하고 조회합니다. */
public interface CampaignVisitSnapshotRepository
    extends JpaRepository<CampaignVisitSnapshot, Long> {}
