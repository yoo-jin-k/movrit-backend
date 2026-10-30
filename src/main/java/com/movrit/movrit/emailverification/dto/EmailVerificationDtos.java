package com.movrit.movrit.emailverification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

/** 이메일 소유 확인 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
public final class EmailVerificationDtos {
  private EmailVerificationDtos() {}

  /**
   * 이메일 소유 확인의 이메일 인증 코드 확인 요청을 전달합니다.
   *
   * @param challengeId 이메일 인증 요청 식별자입니다. null을 허용하지 않습니다.
   * @param code 6자리 이메일 인증 코드이며 로그와 응답에 노출하지 않습니다. 빈 문자열을 허용하지 않습니다.
   */
  public record ConfirmRequest(
      @NotNull Long challengeId, @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code) {}

  /**
   * 이메일 소유 확인의 이메일 인증 요청 접수 결과를 전달합니다.
   *
   * @param challengeId 이메일 인증 요청 식별자입니다.
   * @param maskedEmail 일부를 가린 수신 이메일 표시값입니다.
   * @param expiresAt 인증 요청 만료 시각입니다.
   */
  public record RequestResponse(Long challengeId, String maskedEmail, LocalDateTime expiresAt) {}

  /**
   * 이메일 소유 확인의 인증 상태 응답을 전달합니다.
   *
   * @param emailVerified 이메일 소유 확인 여부이며 휴대폰 본인인증과 구분합니다.
   * @param emailVerifiedAt 이메일 소유 확인 완료 시각입니다.
   * @param identityVerified 외부 본인인증 여부이며 이메일 인증과 구분합니다.
   * @param applicationEligibilityPolicy 서버가 적용한 캠페인 신청 자격 정책 코드입니다.
   */
  public record StatusResponse(
      boolean emailVerified,
      LocalDateTime emailVerifiedAt,
      boolean identityVerified,
      String applicationEligibilityPolicy) {}
}
