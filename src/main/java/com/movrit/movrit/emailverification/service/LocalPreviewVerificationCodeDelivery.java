package com.movrit.movrit.emailverification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 시연용 인증 코드 전달 방식입니다. 실제 메일을 보내지 않고, 발급한 코드를 인증 요청 응답에 담아 화면에 보여줍니다.
 *
 * <p>SMTP 계정 없이도 새로 가입한 회원이 이메일 인증 → 캠페인 신청까지 시연할 수 있게 하기 위한 기능입니다. 코드가 화면에 노출되므로 통합 프로필
 * (integration)에서 movrit.email.delivery=local-preview 로 명시했을 때만 만들어집니다. 다른 프로필에서 이 값을 쓰면 전달 방식이 없어
 * 서버가 시작되지 않으므로, 운영 환경에서 실수로 켜질 수 없습니다.
 */
@Slf4j
@Component
@Profile("integration")
@ConditionalOnProperty(name = "movrit.email.delivery", havingValue = "local-preview")
public class LocalPreviewVerificationCodeDelivery implements VerificationCodeDelivery {

  /** 메일을 보내지 않고, 시연 모드로 코드를 발급했다는 사실만 기록합니다(코드 값과 전체 이메일은 로그에 남기지 않습니다). */
  @Override
  public void send(String email, String code, int expiresInMinutes) {
    log.info("로컬 시연 모드: 이메일 인증 코드를 화면 표시용으로 발급했습니다(유효 {}분).", expiresInMinutes);
  }

  /** 시연 모드에서는 화면에 코드를 보여줍니다. */
  @Override
  public boolean exposesCodeForLocalPreview() {
    return true;
  }
}
