package com.movrit.movrit.campaignmedia.repository;

import com.movrit.movrit.campaignmedia.entity.CampaignMediaState;
import org.springframework.data.jpa.repository.JpaRepository;

/** 캠페인별 이미지·방문 장소 구성 상태를 저장하고 조회합니다. */
public interface CampaignMediaStateRepository extends JpaRepository<CampaignMediaState, Long> {}
