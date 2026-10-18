package com.movrit.movrit.emailverification.controller;

import com.movrit.movrit.emailverification.dto.EmailVerificationDtos;
import com.movrit.movrit.emailverification.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email-verifications")
public class EmailVerificationController {
  private final EmailVerificationService service;

  @PostMapping("/requests")
  public ResponseEntity<EmailVerificationDtos.RequestResponse> request(
      @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.request(memberId(jwt)));
  }

  @PostMapping("/confirm")
  public EmailVerificationDtos.StatusResponse confirm(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody EmailVerificationDtos.ConfirmRequest request) {
    return service.confirm(memberId(jwt), request);
  }

  @GetMapping("/status")
  public EmailVerificationDtos.StatusResponse status(@AuthenticationPrincipal Jwt jwt) {
    return service.status(memberId(jwt));
  }

  private Long memberId(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
