package com.movrit.movrit.admin.controller;

import com.movrit.movrit.admin.dto.AdminDtos;
import com.movrit.movrit.admin.service.AdminDashboardService;
import com.movrit.movrit.common.security.JwtMemberId;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영 현황의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@Tag(name = "운영자 · 운영 현황")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
  private final AdminDashboardService service;

  /** 대시보드 집계를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping
  public AdminDtos.Dashboard get(@AuthenticationPrincipal Jwt jwt) {
    return service.get(JwtMemberId.from(jwt));
  }
}
