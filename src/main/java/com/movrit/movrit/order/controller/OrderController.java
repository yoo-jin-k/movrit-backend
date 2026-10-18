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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService orderService;

  @PostMapping
  public ResponseEntity<OrderDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(orderService.create(memberId(jwt), request));
  }

  @GetMapping("/mine")
  public List<OrderDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
    return orderService.findMine(memberId(jwt));
  }

  @GetMapping("/seller")
  public List<OrderDtos.Response> findSellerOrders(@AuthenticationPrincipal Jwt jwt) {
    return orderService.findSellerOrders(memberId(jwt));
  }

  @GetMapping("/{id}")
  public OrderDtos.Response findById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return orderService.findById(memberId(jwt), id);
  }

  @PutMapping("/{id}/receiver")
  public OrderDtos.Response updateReceiver(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @Valid @RequestBody OrderDtos.ReceiverUpdateRequest request) {
    return orderService.updateReceiver(memberId(jwt), id, request);
  }

  @PatchMapping("/{id}/cancel")
  public OrderDtos.Response cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return orderService.cancel(memberId(jwt), id);
  }

  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
