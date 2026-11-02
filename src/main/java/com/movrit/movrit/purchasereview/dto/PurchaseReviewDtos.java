package com.movrit.movrit.purchasereview.dto;

import com.movrit.movrit.purchasereview.entity.CampaignPurchaseProof;
import com.movrit.movrit.purchasereview.entity.CampaignPurchaseReview;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 구매평 캠페인 API의 요청·응답 형식입니다. */
public final class PurchaseReviewDtos {
  private PurchaseReviewDtos() {}

  /**
   * 업체가 저장하는 구매 안내입니다.
   *
   * @param storeName 구매처 이름(예: 네이버 스마트스토어)
   * @param productUrl 상품 페이지 주소(http·https)
   * @param productPrice 구매처 판매가(원)
   * @param paybackAmount 구매평 승인 뒤 돌려줄 금액(원). 판매가보다 클 수 없습니다.
   * @param purchaseGuide 구매 방법·옵션·구매평 작성 안내(선택)
   */
  public record InfoRequest(
      @NotBlank @Size(max = 50) String storeName,
      @NotBlank @Size(max = 500) String productUrl,
      @NotNull @Min(0) @Max(100_000_000) Integer productPrice,
      @NotNull @Min(0) @Max(100_000_000) Integer paybackAmount,
      @Size(max = 2000) String purchaseGuide) {}

  /**
   * 화면에 보여줄 구매 안내입니다.
   *
   * @param finalPrice 참여자가 실제로 부담하는 금액(판매가 - 환급액)
   */
  public record Info(
      Long campaignId,
      String storeName,
      String productUrl,
      int productPrice,
      int paybackAmount,
      int finalPrice,
      String purchaseGuide) {
    public static Info from(CampaignPurchaseReview value) {
      return new Info(
          value.getCampaignId(),
          value.getStoreName(),
          value.getProductUrl(),
          value.getProductPrice(),
          value.getPaybackAmount(),
          value.getProductPrice() - value.getPaybackAmount(),
          value.getPurchaseGuide());
    }
  }

  /**
   * 구매 인증·환급 상태입니다.
   *
   * @param paybackStatus PENDING(환급 대기) 또는 PAID(환급 완료)
   */
  public record Proof(
      Long applicationId,
      String orderNumber,
      int paidAmount,
      LocalDateTime submittedAt,
      String paybackStatus,
      LocalDateTime paybackPaidAt,
      String paybackNote) {
    public static Proof from(CampaignPurchaseProof value) {
      return new Proof(
          value.getApplicationId(),
          value.getOrderNumber(),
          value.getPaidAmount(),
          value.getSubmittedAt(),
          value.getPaybackStatus(),
          value.getPaybackPaidAt(),
          value.getPaybackNote());
    }
  }

  /**
   * 업체 '환급 관리' 목록 한 건입니다.
   *
   * @param paybackStatus NO_PROOF(구매 인증 전)·PENDING(환급 대기)·PAID(환급 완료)
   * @param payable 지금 환급 완료를 기록할 수 있는지(구매평 승인 완료 + 환급 대기)
   */
  public record PaybackRow(
      Long applicationId,
      Long campaignId,
      String campaignTitle,
      String creatorNickname,
      String applicationStatus,
      String orderNumber,
      Integer paidAmount,
      LocalDateTime proofSubmittedAt,
      Integer paybackAmount,
      String paybackStatus,
      LocalDateTime paybackPaidAt,
      String paybackNote,
      boolean payable) {}

  /** 환급 완료 기록 요청입니다. 메모는 선택이며 200자까지 받습니다. */
  public record PaybackRequest(@Size(max = 200) String note) {}
}
