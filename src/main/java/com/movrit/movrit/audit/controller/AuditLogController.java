package com.movrit.movrit.audit.controller;

import com.movrit.movrit.audit.dto.AuditDtos;
import com.movrit.movrit.audit.service.AuditLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {
  private final AuditLogService service;

  @GetMapping
  public List<AuditDtos.Response> recent(@AuthenticationPrincipal Jwt jwt) {
    return service.findRecent(Long.valueOf(jwt.getSubject()));
  }
}
