package com.movrit.movrit.inquiry.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.inquiry.dto.InquiryDtos;
import com.movrit.movrit.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 문의의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@Tag(name = "공통 · 문의")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {
  private final InquiryService service;

  /** 1:1 문의를 등록합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping
  public ResponseEntity<InquiryDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody InquiryDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(memberId(jwt), request));
  }

  /** 로그인 회원에게 연결된 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/mine")
  public List<InquiryDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(memberId(jwt));
  }

  /** 관리 대상 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/admin")
  public List<InquiryDtos.Response> all(@AuthenticationPrincipal Jwt jwt) {
    return service.findAll(memberId(jwt));
  }

  /** 문의 답변 등록을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PatchMapping("/{inquiryId}/answer")
  public InquiryDtos.Response answer(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long inquiryId,
      @Valid @RequestBody InquiryDtos.AnswerRequest request) {
    return service.answer(memberId(jwt), inquiryId, request);
  }

  /** 종료 처리를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{inquiryId}/close")
  public InquiryDtos.Response close(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long inquiryId) {
    return service.close(memberId(jwt), inquiryId);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
