package com.movrit.movrit.phoneverification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** 문자 발송 설정(movrit.phone.delivery)이 없을 때 쓰는 기본 구현입니다. 보낸 것처럼 보이지 않도록 503으로 안내합니다. */
@Component
@ConditionalOnProperty(
    name = "movrit.phone.delivery",
    havingValue = "unconfigured",
    matchIfMissing = true)
public class UnconfiguredPhoneCodeSender implements PhoneCodeSender {

  @Override
  public void send(String phoneDigits, String code, int expiresInMinutes) {
    throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "휴대폰 인증번호 발송이 아직 준비되지 않았습니다.");
  }
}
