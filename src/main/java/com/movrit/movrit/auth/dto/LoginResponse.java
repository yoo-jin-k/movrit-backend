package com.movrit.movrit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 로그인 및 소셜 인증 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
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

  /** 헤더와 프로필에서 사용하는 회원 표시용 닉네임입니다. */
  private String nickname;

  /** 헤더에서 사용하는 재인코딩된 프로필 사진 URL이며 사진이 없으면 null입니다. */
  private String avatarUrl;
}
