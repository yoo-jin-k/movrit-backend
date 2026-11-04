package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.CampaignPeriodExtension;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기간 연장 기록 저장소입니다. */
public interface CampaignPeriodExtensionRepository
    extends JpaRepository<CampaignPeriodExtension, Long> {

  /** 참여자 한 명의 연장 기록입니다(최근 순). */
  List<CampaignPeriodExtension> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

  /** 참여자 한 명의 연장 기록입니다(오래된 순, 횟수 기준 계산용). */
  List<CampaignPeriodExtension> findAllByMemberIdOrderByCreatedAtAsc(Long memberId);

  /** 신청 하나의 연장 기록입니다(최근 순). */
  List<CampaignPeriodExtension> findAllByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
