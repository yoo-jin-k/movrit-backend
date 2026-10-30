package com.movrit.movrit.emailverification.service;

/** 이메일 인증 코드를 실제로 보내는 방법을 추상화합니다. SMTP 발송과 미설정(발송 안 함) 구현이 있습니다. */
public interface VerificationCodeDelivery {
  /** 인증 코드와 유효 시간을 지정한 이메일 주소로 보냅니다. */
  void send(String email, String code, int expiresInMinutes);
}
