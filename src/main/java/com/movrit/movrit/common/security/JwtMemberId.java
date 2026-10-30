package com.movrit.movrit.common.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.jwt.Jwt;

/** 인증 토큰의 subject를 MOVRIT 회원 식별자로 변환합니다. */
@UtilityClass
public class JwtMemberId {
  /** 인증 토큰의 subject 값을 회원 ID(Long)로 바꿉니다. 컨트롤러는 요청 본문이 아닌 이 값으로 요청자를 판단합니다. */
  public Long from(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
