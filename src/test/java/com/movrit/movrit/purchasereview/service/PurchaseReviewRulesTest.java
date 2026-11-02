package com.movrit.movrit.purchasereview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.movrit.movrit.purchasereview.entity.CampaignPurchaseProof;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** 구매평 캠페인의 기본 규칙(상품 주소 형식, 구매 인증·환급 순서)을 확인합니다. */
class PurchaseReviewRulesTest {

  @Test
  void productAddressMustBeWebAddress() {
    assertThatCode(
            () -> PurchaseReviewService.requireWebAddress("https://smartstore.naver.com/a/1"))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> PurchaseReviewService.requireWebAddress("javascript:alert(1)"))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> PurchaseReviewService.requireWebAddress("smartstore.naver.com"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void proofCanBeReplacedUntilPaybackThenLocked() {
    CampaignPurchaseProof proof = new CampaignPurchaseProof(1L);
    assertThat(proof.submit("A-1", 19_900, "first.jpg")).isNull();
    // 다시 제출하면 이전 캡처 키를 돌려줘 파일을 지울 수 있게 합니다.
    assertThat(proof.submit("A-2", 19_900, "second.jpg")).isEqualTo("first.jpg");
    assertThat(proof.getOrderNumber()).isEqualTo("A-2");

    proof.markPaid("계좌 이체");
    assertThat(proof.isPaid()).isTrue();
    assertThat(proof.getPaybackPaidAt()).isNotNull();
    assertThatThrownBy(() -> proof.markPaid(null)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> proof.submit("A-3", 1, "third.jpg"))
        .isInstanceOf(IllegalStateException.class);
  }
}
