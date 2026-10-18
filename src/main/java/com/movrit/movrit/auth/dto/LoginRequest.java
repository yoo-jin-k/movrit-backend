package com.movrit.movrit.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그인 및 소셜 인증의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
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
