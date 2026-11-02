package com.movrit.movrit.purchasereview.repository;

import com.movrit.movrit.purchasereview.entity.CampaignPurchaseReview;
import org.springframework.data.jpa.repository.JpaRepository;

/** 구매평 캠페인의 구매 안내를 조회합니다(키: 캠페인 ID). */
public interface CampaignPurchaseReviewRepository
    extends JpaRepository<CampaignPurchaseReview, Long> {}
