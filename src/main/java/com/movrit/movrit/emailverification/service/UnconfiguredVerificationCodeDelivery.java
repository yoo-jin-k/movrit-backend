package com.movrit.movrit.emailverification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(
    name = "movrit.email.delivery",
    havingValue = "unconfigured",
    matchIfMissing = true)
public class UnconfiguredVerificationCodeDelivery implements VerificationCodeDelivery {
  @Override
  public void send(String email, String code, int expiresInMinutes) {
    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "이메일 발송 설정이 아직 구성되지 않았습니다.");
  }
}
