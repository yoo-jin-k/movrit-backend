package com.movrit.movrit.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그인 및 소셜 인증 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
@Getter
@NoArgsConstructor
public class LoginRequest {

  /** 이메일 주소입니다. 빈 문자열을 허용하지 않습니다. */
  @NotBlank(message = "이메일 필수.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  private String email;

  /** 입력 비밀번호이며 로그와 응답에 노출하지 않습니다. 빈 문자열을 허용하지 않습니다. */
  @NotBlank(message = "비밀번호 필수.")
  private String password;
}
