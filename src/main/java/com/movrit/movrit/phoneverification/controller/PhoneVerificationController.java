package com.movrit.movrit.phoneverification.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.phoneverification.dto.PhoneVerificationDtos;
import com.movrit.movrit.phoneverification.service.PhoneVerificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 휴대폰 번호 인증 API입니다. 회원은 로그인 토큰으로만 확인하고 요청 본문의 회원 ID는 받지 않습니다.
 *
 * <ul>
 *   <li>POST /api/phone-verifications/requests: 인증번호 발송(202)
 *   <li>POST /api/phone-verifications/confirm: 인증번호 확인 → 연락처 인증됨
 *   <li>GET /api/phone-verifications/status: 인증 상태
 * </ul>
 */
@Tag(name = "공통 · 휴대폰 인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/phone-verifications")
public class PhoneVerificationController {
  private final PhoneVerificationService service;

  /** 입력한 번호로 인증번호를 보냅니다. 로컬 시연 모드에서만 응답에 인증번호가 담깁니다. */
  @PostMapping("/requests")
  public ResponseEntity<PhoneVerificationDtos.SendResponse> send(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody PhoneVerificationDtos.SendRequest request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(service.send(JwtMemberId.from(jwt), request));
  }

  /** 받은 인증번호를 확인해 회원 연락처를 '인증됨'으로 바꿉니다. */
  @PostMapping("/confirm")
  public PhoneVerificationDtos.StatusResponse confirm(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody PhoneVerificationDtos.ConfirmRequest request) {
    return service.confirm(JwtMemberId.from(jwt), request);
  }

  /** 휴대폰 인증 상태를 조회합니다. */
  @GetMapping("/status")
  public PhoneVerificationDtos.StatusResponse status(@AuthenticationPrincipal Jwt jwt) {
    return service.status(JwtMemberId.from(jwt));
  }
}
