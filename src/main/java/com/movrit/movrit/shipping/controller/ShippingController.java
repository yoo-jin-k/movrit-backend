package com.movrit.movrit.shipping.controller;

import com.movrit.movrit.shipping.dto.ShippingDtos;
import com.movrit.movrit.shipping.service.ShippingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipments")
public class ShippingController {
  private final ShippingService shippingService;

  @PostMapping("/orders/{orderId}")
  public ResponseEntity<ShippingDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
    return ResponseEntity.status(HttpStatus.CREATED).body(shippingService.create(id(jwt), orderId));
  }

  @GetMapping("/mine")
  public List<ShippingDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return shippingService.findMine(id(jwt));
  }

  @GetMapping("/buyer")
  public List<ShippingDtos.Response> buyer(@AuthenticationPrincipal Jwt jwt) {
    return shippingService.findBuyerShipments(id(jwt));
  }

  @PatchMapping("/{shippingId}/prepare")
  public ShippingDtos.Response prepare(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long shippingId) {
    return shippingService.prepare(id(jwt), shippingId);
  }

  @PatchMapping("/{shippingId}/ship")
  public ShippingDtos.Response ship(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long shippingId,
      @Valid @RequestBody ShippingDtos.ShipRequest request) {
    return shippingService.ship(id(jwt), shippingId, request);
  }

  @PatchMapping("/{shippingId}/deliver")
  public ShippingDtos.Response deliver(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long shippingId) {
    return shippingService.deliver(id(jwt), shippingId);
  }

  private Long id(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
