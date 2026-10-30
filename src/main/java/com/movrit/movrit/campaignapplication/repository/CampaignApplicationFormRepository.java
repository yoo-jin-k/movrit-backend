package com.movrit.movrit.campaignapplication.repository;

import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 캠페인별 신청서 양식을 저장하고 조회합니다. 캠페인 하나에 양식은 하나입니다. */
public interface CampaignApplicationFormRepository
    extends JpaRepository<CampaignApplicationForm, Long> {
  Optional<CampaignApplicationForm> findByCampaignId(Long campaignId);
}
