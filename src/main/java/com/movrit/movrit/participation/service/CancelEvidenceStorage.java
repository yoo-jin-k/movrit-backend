package com.movrit.movrit.participation.service;

import com.movrit.movrit.common.file.PrivateImageStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 협의 취소 증빙 이미지(업체와 나눈 대화 캡처 등)를 저장합니다(비공개 위치 app.private-upload-dir 아래 cancel-evidence).
 *
 * <p>대화 내용이 담길 수 있어 공개 주소가 없고, 요청한 참여자·캠페인 업체·운영자 API로만 내려갑니다.
 */
@Component
public class CancelEvidenceStorage extends PrivateImageStorage {
  public CancelEvidenceStorage(
      @Value("${app.private-upload-dir:uploads-private}") String uploadDir) {
    super(uploadDir, "cancel-evidence", "증빙 이미지");
  }
}
