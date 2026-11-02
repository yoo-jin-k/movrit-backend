package com.movrit.movrit.purchasereview.repository;

import com.movrit.movrit.purchasereview.entity.CampaignPurchaseProof;
import org.springframework.data.jpa.repository.JpaRepository;

/** 구매 인증·환급 기록을 조회합니다(키: 신청 ID). */
public interface CampaignPurchaseProofRepository
    extends JpaRepository<CampaignPurchaseProof, Long> {}
