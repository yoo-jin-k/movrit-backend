package com.movrit.movrit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 로그인 및 소셜 인증의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
@Getter
@AllArgsConstructor
public class LoginResponse {

  /** API 인증에 사용하는 접근 토큰이며 로그에 노출하지 않습니다. */
  private String accessToken;

  /** 접근 토큰 재발급에 사용하는 토큰이며 로그에 노출하지 않습니다. */
  private String refreshToken;

  /** 접근 토큰의 인증 방식입니다. */
  private String tokenType;

  /** 회원 식별자입니다. */
  private Long memberId;

  /** 이메일 주소입니다. */
  private String email;

  /** 표시 이름입니다. */
  private String name;
}
