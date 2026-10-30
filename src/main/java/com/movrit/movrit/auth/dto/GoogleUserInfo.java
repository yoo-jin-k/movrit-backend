package com.movrit.movrit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 로그인 및 소셜 인증 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
@Getter
@AllArgsConstructor
public class GoogleUserInfo {

  /** 소셜 제공자가 발급한 사용자 식별자이며 MOVRIT 회원 ID와 구분합니다. */
  private String providerId;

  /** 이메일 주소입니다. */
  private String email;

  /** 표시 이름입니다. */
  private String name;
}
