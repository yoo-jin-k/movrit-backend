package com.movrit.movrit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 로그인 및 소셜 인증의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
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
