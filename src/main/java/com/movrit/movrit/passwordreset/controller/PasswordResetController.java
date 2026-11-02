package com.movrit.movrit.passwordreset.controller;

import com.movrit.movrit.passwordreset.dto.PasswordResetDtos;
import com.movrit.movrit.passwordreset.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인하지 않은 상태에서 쓰는 비밀번호 찾기 API입니다.
 *
 * <ul>
 *   <li>POST /api/auth/password/forgot: 재설정 메일 요청(가입 여부와 관계없이 같은 안내)
 *   <li>GET /api/auth/password/reset/validate?token=: 링크 확인
 *   <li>POST /api/auth/password/reset: 새 비밀번호 저장
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/password")
public class PasswordResetController {
  private final PasswordResetService service;

  @PostMapping("/forgot")
  public PasswordResetDtos.ForgotResponse forgot(
      @Valid @RequestBody PasswordResetDtos.ForgotRequest request) {
    return service.requestReset(request.email());
  }

  @GetMapping("/reset/validate")
  public PasswordResetDtos.ValidateResponse validate(@RequestParam String token) {
    return service.validate(token);
  }

  @PostMapping("/reset")
  public PasswordResetDtos.ResetResponse reset(
      @Valid @RequestBody PasswordResetDtos.ResetRequest request) {
    return service.reset(request.token(), request.newPassword());
  }
}
