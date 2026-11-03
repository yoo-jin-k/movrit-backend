package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.CampaignUnselectedRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 업체 미선정 기록 저장소입니다(캠페인 ID가 기본 키). */
public interface CampaignUnselectedRecordRepository
    extends JpaRepository<CampaignUnselectedRecord, Long> {
  List<CampaignUnselectedRecord> findAllBySellerMemberIdOrderByRecordedAtDesc(Long sellerMemberId);

  List<CampaignUnselectedRecord> findTop200ByOrderByRecordedAtDesc();
}
