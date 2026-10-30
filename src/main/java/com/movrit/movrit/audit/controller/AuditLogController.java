package com.movrit.movrit.audit.controller;

import com.movrit.movrit.audit.dto.AuditDtos;
import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.common.security.JwtMemberId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 감사 이력의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {
  private final AuditLogService service;

  /** 최근 감사 이력을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping
  public List<AuditDtos.Response> recent(@AuthenticationPrincipal Jwt jwt) {
    return service.findRecent(JwtMemberId.from(jwt));
  }
}
