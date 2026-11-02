package com.movrit.movrit.passwordreset.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 비밀번호 찾기·재설정 요청과 응답 형식입니다. */
public final class PasswordResetDtos {
  private PasswordResetDtos() {}

  /** 재설정 메일 요청입니다. */
  public record ForgotRequest(@NotBlank @Email @Size(max = 100) String email) {}

  /**
   * 재설정 메일 요청 결과입니다. 가입 여부와 관계없이 같은 안내를 돌려줍니다.
   *
   * @param devResetToken 로컬 시연 모드에서만 채워지는 재설정 토큰(실제 메일 발송 모드에서는 항상 null)
   */
  public record ForgotResponse(String message, String devResetToken) {}

  /** 링크 확인 결과입니다. */
  public record ValidateResponse(boolean valid) {}

  /** 새 비밀번호 저장 요청입니다. 비밀번호 규칙은 비밀번호 변경과 같습니다. */
  public record ResetRequest(
      @NotBlank @Size(max = 200) String token,
      @NotBlank
          @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해 주세요.")
          @Pattern(
              regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).+$",
              message = "비밀번호에는 영문자와 숫자를 각각 하나 이상 포함해 주세요.")
          String newPassword) {}

  /** 재설정 완료 안내입니다. */
  public record ResetResponse(String message) {}
}
