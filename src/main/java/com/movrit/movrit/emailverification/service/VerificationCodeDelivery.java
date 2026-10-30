package com.movrit.movrit.emailverification.service;

/**
 * 이메일 인증 코드를 실제로 보내는 방법을 추상화합니다.
 *
 * <p>구현: SMTP 발송(smtp), 미설정(unconfigured, 503 안내), 로컬 시연(local-preview, 통합 프로필 전용으로 화면에 코드 표시).
 */
public interface VerificationCodeDelivery {
  /** 인증 코드와 유효 시간을 지정한 이메일 주소로 보냅니다. */
  void send(String email, String code, int expiresInMinutes);

  /**
   * 발급한 코드를 요청 응답에 담아 화면에 보여줘도 되는지 여부입니다.
   *
   * <p>실제 메일을 보내지 않는 로컬 시연 모드에서만 true입니다. 운영 발송 방식은 항상 false여야 하며 기본값도 false입니다.
   */
  default boolean exposesCodeForLocalPreview() {
    return false;
  }
}
