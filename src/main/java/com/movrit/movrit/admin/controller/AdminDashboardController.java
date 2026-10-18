package com.movrit.movrit.admin.controller;

import com.movrit.movrit.admin.dto.AdminDtos;
import com.movrit.movrit.admin.service.AdminDashboardService;
import com.movrit.movrit.common.security.JwtMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
  private final AdminDashboardService service;

  @GetMapping
  public AdminDtos.Dashboard get(@AuthenticationPrincipal Jwt jwt) {
    return service.get(JwtMemberId.from(jwt));
  }
}
