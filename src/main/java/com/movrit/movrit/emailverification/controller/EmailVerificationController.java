package com.movrit.movrit.emailverification.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.emailverification.dto.EmailVerificationDtos;
import com.movrit.movrit.emailverification.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 이메일 소유 확인의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email-verifications")
public class EmailVerificationController {
  private final EmailVerificationService service;

  /** 이메일 인증 요청을 접수합니다. 실제 메일 전달 여부는 발송 환경과 서비스를 별도로 확인해야 합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PostMapping("/requests")
  public ResponseEntity<EmailVerificationDtos.RequestResponse> request(
      @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.request(memberId(jwt)));
  }

  /**
   * 인증 요청 ID와 6자리 코드를 서비스에서 확인하여 이메일 소유 확인 상태를 갱신합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저
   * 검사한 뒤 처리합니다.
   */
  @PostMapping("/confirm")
  public EmailVerificationDtos.StatusResponse confirm(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody EmailVerificationDtos.ConfirmRequest request) {
    return service.confirm(memberId(jwt), request);
  }

  /** 현재 상태를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/status")
  public EmailVerificationDtos.StatusResponse status(@AuthenticationPrincipal Jwt jwt) {
    return service.status(memberId(jwt));
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
