package com.movrit.movrit.settlement.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.settlement.dto.SettlementDtos;
import com.movrit.movrit.settlement.service.SettlementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements")
public class SettlementController {
  private final SettlementService service;

  @PostMapping
  public ResponseEntity<SettlementDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SettlementDtos.CreateRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(id(jwt), r));
  }

  @GetMapping("/mine")
  public List<SettlementDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(id(jwt));
  }

  @GetMapping
  public List<SettlementDtos.Response> all(@AuthenticationPrincipal Jwt jwt) {
    return service.findAll(id(jwt));
  }

  @PatchMapping("/{settlementId}/confirm")
  public SettlementDtos.Response confirm(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long settlementId) {
    return service.confirm(id(jwt), settlementId);
  }

  @PatchMapping("/{settlementId}/pay")
  public SettlementDtos.Response pay(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long settlementId) {
    return service.pay(id(jwt), settlementId);
  }

  @PatchMapping("/{settlementId}/cancel")
  public SettlementDtos.Response cancel(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long settlementId) {
    return service.cancel(id(jwt), settlementId);
  }

  private Long id(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
