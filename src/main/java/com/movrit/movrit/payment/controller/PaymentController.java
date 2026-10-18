package com.movrit.movrit.payment.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.payment.dto.PaymentDtos;
import com.movrit.movrit.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 결제 기록의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
  private final PaymentService paymentService;

  /** 결제 기록 관련 생성을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PostMapping
  public ResponseEntity<PaymentDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PaymentDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(id(jwt), request));
  }

  /** 결제 기록 관련 로그인 회원에게 연결된 정보를 조회합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @GetMapping("/mine")
  public List<PaymentDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return paymentService.findMine(id(jwt));
  }

  /** 결제 기록 관련 승인 처리를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PatchMapping("/{paymentId}/approve")
  public PaymentDtos.Response approve(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long paymentId,
      @Valid @RequestBody PaymentDtos.ApproveRequest request) {
    return paymentService.approve(id(jwt), paymentId, request);
  }

  /** 결제 기록 관련 결제 실패 결과 기록을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PatchMapping("/{paymentId}/fail")
  public PaymentDtos.Response fail(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long paymentId,
      @Valid @RequestBody PaymentDtos.FailRequest request) {
    return paymentService.fail(id(jwt), paymentId, request);
  }

  /** 결제 기록 관련 환불 상태 처리를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/{paymentId}/refund")
  public PaymentDtos.Response refund(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long paymentId) {
    return paymentService.refund(id(jwt), paymentId);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long id(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
