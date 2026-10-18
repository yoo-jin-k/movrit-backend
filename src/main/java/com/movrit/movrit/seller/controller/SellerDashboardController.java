package com.movrit.movrit.seller.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.seller.dto.SellerDashboardDtos;
import com.movrit.movrit.seller.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sellers/dashboard")
public class SellerDashboardController {
  private final SellerDashboardService service;

  @GetMapping
  public SellerDashboardDtos.Response get(@AuthenticationPrincipal Jwt jwt) {
    return service.get(JwtMemberId.from(jwt));
  }
}
