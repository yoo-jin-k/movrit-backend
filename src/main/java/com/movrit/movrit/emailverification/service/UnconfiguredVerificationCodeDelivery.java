package com.movrit.movrit.emailverification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 이메일 발송 설정(movrit.email.delivery)이 없을 때 쓰는 기본 구현입니다. 실제 메일을 보내지 않고 "발송 설정 없음" 오류를 돌려줘, 인증 메일이 간
 * 것처럼 보이지 않게 합니다.
 */
@Component
@ConditionalOnProperty(
    name = "movrit.email.delivery",
    havingValue = "unconfigured",
    matchIfMissing = true)
/** 메일 발송 설정이 없을 때 쓰는 발송기입니다. 코드를 보내지 않고 '발송 설정이 필요합니다'로 안내합니다. */
public class UnconfiguredVerificationCodeDelivery implements VerificationCodeDelivery {
  /** 항상 503 오류를 냅니다. 로컬 환경에서 인증 메일이 실제로 발송됐다고 오해하지 않도록 하기 위함입니다. */
  @Override
  public void send(String email, String code, int expiresInMinutes) {
    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "이메일 발송 설정이 아직 구성되지 않았습니다.");
  }
}
