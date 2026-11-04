package com.movrit.movrit.phoneverification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 시연용 인증번호 전달 방식입니다. 문자를 보내지 않고, 발급한 번호를 발송 응답에 담아 화면에 보여줍니다.
 *
 * <p>발송 계약 없이 휴대폰 인증을 시연하기 위한 기능입니다. 번호가 화면에 보이므로 통합 프로필(integration)에서
 * movrit.phone.delivery=local-preview 로 지정했을 때만 만들어집니다. 다른 프로필에서 이 값을 쓰면 발송 방식이 없어 서버가 시작되지 않으므로
 * 운영에서 실수로 켜질 수 없습니다.
 */
@Slf4j
@Component
@Profile("integration")
@ConditionalOnProperty(name = "movrit.phone.delivery", havingValue = "local-preview")
public class LocalPreviewPhoneCodeSender implements PhoneCodeSender {

  /** 문자를 보내지 않고 발급 사실만 남깁니다(번호·인증번호는 로그에 남기지 않습니다). */
  @Override
  public void send(String phoneDigits, String code, int expiresInMinutes) {
    log.info("로컬 시연 모드: 휴대폰 인증번호를 화면 표시용으로 발급했습니다(유효 {}분).", expiresInMinutes);
  }

  @Override
  public boolean exposesCodeForLocalPreview() {
    return true;
  }
}
