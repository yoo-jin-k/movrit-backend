package com.movrit.movrit.order.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.order.dto.OrderDtos;
import com.movrit.movrit.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 주문의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService orderService;

  /** 생성을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping
  public ResponseEntity<OrderDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(orderService.create(memberId(jwt), request));
  }

  /** 로그인 회원에게 연결된 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/mine")
  public List<OrderDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
    return orderService.findMine(memberId(jwt));
  }

  /** 업체에 연결된 주문을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/seller")
  public List<OrderDtos.Response> findSellerOrders(@AuthenticationPrincipal Jwt jwt) {
    return orderService.findSellerOrders(memberId(jwt));
  }

  /** 식별자로 상세 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/{id}")
  public OrderDtos.Response findById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return orderService.findById(memberId(jwt), id);
  }

  /** 주문 수령인과 배송지를 수정합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PutMapping("/{id}/receiver")
  public OrderDtos.Response updateReceiver(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @Valid @RequestBody OrderDtos.ReceiverUpdateRequest request) {
    return orderService.updateReceiver(memberId(jwt), id, request);
  }

  /** 취소 처리를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{id}/cancel")
  public OrderDtos.Response cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return orderService.cancel(memberId(jwt), id);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
