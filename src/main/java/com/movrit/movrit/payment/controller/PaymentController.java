package com.movrit.movrit.payment.controller;

import com.movrit.movrit.payment.dto.PaymentDtos;
import com.movrit.movrit.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
  private final PaymentService paymentService;

  @PostMapping
  public ResponseEntity<PaymentDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PaymentDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(id(jwt), request));
  }

  @GetMapping("/mine")
  public List<PaymentDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return paymentService.findMine(id(jwt));
  }

  @PatchMapping("/{paymentId}/approve")
  public PaymentDtos.Response approve(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long paymentId,
      @Valid @RequestBody PaymentDtos.ApproveRequest request) {
    return paymentService.approve(id(jwt), paymentId, request);
  }

  @PatchMapping("/{paymentId}/fail")
  public PaymentDtos.Response fail(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long paymentId,
      @Valid @RequestBody PaymentDtos.FailRequest request) {
    return paymentService.fail(id(jwt), paymentId, request);
  }

  @PatchMapping("/{paymentId}/refund")
  public PaymentDtos.Response refund(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long paymentId) {
    return paymentService.refund(id(jwt), paymentId);
  }

  private Long id(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
