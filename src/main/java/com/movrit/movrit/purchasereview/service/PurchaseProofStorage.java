package com.movrit.movrit.purchasereview.service;

import com.movrit.movrit.common.file.PrivateImageStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 구매 인증용 '주문 내역 캡처'를 저장합니다(비공개 위치 app.private-upload-dir 아래 purchase-proof).
 *
 * <p>캡처에는 주문자 정보가 있을 수 있어 공개 주소가 없고, 참여자 본인·캠페인 업체·운영자 API로만 내려갑니다. 검사·저장 규칙은 {@link
 * PrivateImageStorage}를 따릅니다.
 */
@Component
public class PurchaseProofStorage extends PrivateImageStorage {
  public PurchaseProofStorage(
      @Value("${app.private-upload-dir:uploads-private}") String uploadDir) {
    super(uploadDir, "purchase-proof", "캡처 이미지");
  }
}
