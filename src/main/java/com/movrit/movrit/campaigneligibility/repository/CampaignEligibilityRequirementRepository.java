package com.movrit.movrit.campaigneligibility.repository;

import com.movrit.movrit.campaigneligibility.entity.CampaignEligibilityRequirement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 캠페인별 신청 조건을 저장하고 조회합니다. */
public interface CampaignEligibilityRequirementRepository
    extends JpaRepository<CampaignEligibilityRequirement, Long> {
  Optional<CampaignEligibilityRequirement> findByCampaignId(Long campaignId);

  /** 캠페인의 신청 조건을 해제(삭제)합니다. 이후 해당 캠페인은 채널 지표 없이 신청할 수 있습니다. */
  void deleteByCampaignId(Long campaignId);
}
