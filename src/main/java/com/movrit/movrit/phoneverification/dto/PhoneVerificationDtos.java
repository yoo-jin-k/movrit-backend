package com.movrit.movrit.phoneverification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 휴대폰 번호 인증 API에서 주고받는 요청·응답 형식입니다. */
public final class PhoneVerificationDtos {
  private PhoneVerificationDtos() {}

  /**
   * 인증번호 발송 요청입니다.
   *
   * @param phone 휴대폰 번호. 010-1234-5678·01012345678 모두 받고 서버가 숫자만 남깁니다.
   */
  public record SendRequest(@NotBlank @Size(max = 20) String phone) {}

  /**
   * 인증번호 확인 요청입니다.
   *
   * @param challengeId 발송 응답의 요청 식별자
   * @param code 6자리 인증번호. 로그와 응답에 남기지 않습니다.
   */
  public record ConfirmRequest(
      @NotNull Long challengeId, @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code) {}

  /**
   * 인증번호 발송 결과입니다.
   *
   * @param maskedPhone 가운데를 가린 번호(010-****-5678)
   * @param localPreviewCode 로컬 시연 모드에서만 채우는 화면 표시용 인증번호. 실제 발송 환경에서는 항상 null입니다.
   */
  public record SendResponse(
      Long challengeId, String maskedPhone, LocalDateTime expiresAt, String localPreviewCode) {}

  /**
   * 휴대폰 인증 상태입니다.
   *
   * @param maskedPhone 인증한 번호(가운데 가림). 인증 전이면 null
   */
  public record StatusResponse(
      boolean phoneVerified, LocalDateTime phoneVerifiedAt, String maskedPhone) {}
}
