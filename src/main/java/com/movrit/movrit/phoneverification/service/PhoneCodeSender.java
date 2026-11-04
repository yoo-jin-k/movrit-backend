package com.movrit.movrit.phoneverification.service;

/**
 * 휴대폰 인증번호를 실제로 보내는 방법을 바꿔 끼울 수 있게 나눈 부분입니다.
 *
 * <p>구현: 미설정(unconfigured, 503 안내), 로컬 시연(local-preview, 통합 프로필 전용으로 화면에 번호 표시). 실제 발송(카카오 알림톡 → 실패
 * 시 문자)은 발송 대행사 계약·카카오 비즈니스 채널·템플릿 심사가 필요해, 계약 후 이 인터페이스의 구현을 추가하고 movrit.phone.delivery 값으로 고릅니다.
 */
public interface PhoneCodeSender {
  /** 인증번호와 유효 시간을 지정한 번호로 보냅니다. 보내지 못하면 예외를 내 요청을 되돌립니다. */
  void send(String phoneDigits, String code, int expiresInMinutes);

  /** 발급한 번호를 응답에 담아 화면에 보여줘도 되는지입니다. 실제로 보내지 않는 로컬 시연 모드에서만 true입니다. */
  default boolean exposesCodeForLocalPreview() {
    return false;
  }
}
