package com.movrit.movrit.common.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.jwt.Jwt;

/** 인증 토큰의 subject를 MOVRIT 회원 식별자로 변환합니다. */
@UtilityClass
public class JwtMemberId {
  public Long from(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
