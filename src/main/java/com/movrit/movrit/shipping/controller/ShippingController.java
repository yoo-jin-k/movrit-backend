package com.movrit.movrit.shipping.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.shipping.dto.ShippingDtos;
import com.movrit.movrit.shipping.service.ShippingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 배송의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipments")
public class ShippingController {
  private final ShippingService shippingService;

  /** 생성을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PostMapping("/orders/{orderId}")
  public ResponseEntity<ShippingDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
    return ResponseEntity.status(HttpStatus.CREATED).body(shippingService.create(id(jwt), orderId));
  }

  /** 로그인 회원에게 연결된 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/mine")
  public List<ShippingDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return shippingService.findMine(id(jwt));
  }

  /** 구매 회원의 배송 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/buyer")
  public List<ShippingDtos.Response> buyer(@AuthenticationPrincipal Jwt jwt) {
    return shippingService.findBuyerShipments(id(jwt));
  }

  /** 배송 준비 상태 전환을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{shippingId}/prepare")
  public ShippingDtos.Response prepare(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long shippingId) {
    return shippingService.prepare(id(jwt), shippingId);
  }

  /** 택배사와 운송장 번호를 등록하고 발송 처리를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PatchMapping("/{shippingId}/ship")
  public ShippingDtos.Response ship(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long shippingId,
      @Valid @RequestBody ShippingDtos.ShipRequest request) {
    return shippingService.ship(id(jwt), shippingId, request);
  }

  /** 배송 완료 상태 전환을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{shippingId}/deliver")
  public ShippingDtos.Response deliver(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long shippingId) {
    return shippingService.deliver(id(jwt), shippingId);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long id(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
