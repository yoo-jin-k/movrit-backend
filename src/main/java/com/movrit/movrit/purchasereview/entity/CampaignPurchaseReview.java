package com.movrit.movrit.purchasereview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구매평 캠페인의 구매 안내입니다(캠페인당 1건).
 *
 * <p>참여자가 어디서(구매처·상품 주소) 얼마에(판매가) 사고, 구매평 승인 뒤 얼마를 돌려받는지(환급액)를 담습니다. 환급액은 판매가보다 클 수 없습니다.
 */
@Entity
@Table(name = "campaign_purchase_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignPurchaseReview {
  @Id
  @Column(name = "campaign_id")
  private Long campaignId;

  @Column(name = "store_name", nullable = false, length = 50)
  private String storeName;

  @Column(name = "product_url", nullable = false, length = 500)
  private String productUrl;

  @Column(name = "product_price", nullable = false)
  private Integer productPrice;

  @Column(name = "payback_amount", nullable = false)
  private Integer paybackAmount;

  @Column(name = "purchase_guide", length = 2000)
  private String purchaseGuide;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public CampaignPurchaseReview(Long campaignId) {
    this.campaignId = campaignId;
  }

  /** 구매 안내를 바꿉니다. 값 검사는 서비스가 먼저 합니다. */
  public void update(
      String storeName,
      String productUrl,
      int productPrice,
      int paybackAmount,
      String purchaseGuide) {
    this.storeName = storeName;
    this.productUrl = productUrl;
    this.productPrice = productPrice;
    this.paybackAmount = paybackAmount;
    this.purchaseGuide = purchaseGuide;
    this.updatedAt = LocalDateTime.now();
  }
}
