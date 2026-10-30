package com.movrit.movrit.settlement.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.settlement.dto.SettlementDtos;
import com.movrit.movrit.settlement.service.SettlementService;
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

/** 정산 기록의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements")
public class SettlementController {
  private final SettlementService service;

  /** 생성을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping
  public ResponseEntity<SettlementDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SettlementDtos.CreateRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(id(jwt), r));
  }

  /** 로그인 회원에게 연결된 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/mine")
  public List<SettlementDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(id(jwt));
  }

  /** 관리 대상 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping
  public List<SettlementDtos.Response> all(@AuthenticationPrincipal Jwt jwt) {
    return service.findAll(id(jwt));
  }

  /** 확정 처리를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{settlementId}/confirm")
  public SettlementDtos.Response confirm(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long settlementId) {
    return service.confirm(id(jwt), settlementId);
  }

  /** 지급 상태 처리를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{settlementId}/pay")
  public SettlementDtos.Response pay(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long settlementId) {
    return service.pay(id(jwt), settlementId);
  }

  /** 취소 처리를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{settlementId}/cancel")
  public SettlementDtos.Response cancel(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long settlementId) {
    return service.cancel(id(jwt), settlementId);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long id(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
