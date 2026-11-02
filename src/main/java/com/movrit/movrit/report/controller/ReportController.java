package com.movrit.movrit.report.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.report.dto.ReportDtos;
import com.movrit.movrit.report.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신고·제재 API입니다.
 *
 * <ul>
 *   <li>POST /api/reports: 신고 접수(로그인 회원)
 *   <li>GET /api/admin/content-reports: 운영자 신고 목록(최근 200건)
 *   <li>PATCH /api/admin/content-reports/{id}: 운영자 처리(DISMISS·WARN·HIDE·SUSPEND)
 * </ul>
 */
@Tag(name = "공통 · 신고")
@RestController
@RequiredArgsConstructor
public class ReportController {
  private final ReportService service;

  /** 회원이 글·댓글·회원을 신고합니다. */
  @PostMapping("/api/reports")
  @ResponseStatus(HttpStatus.CREATED)
  public ReportDtos.Created create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReportDtos.CreateRequest request) {
    return service.create(JwtMemberId.from(jwt), request);
  }

  /** 운영자가 최근 신고 목록(200건)을 조회합니다. */
  @GetMapping("/api/admin/content-reports")
  public List<ReportDtos.Item> list(@AuthenticationPrincipal Jwt jwt) {
    return service.list(JwtMemberId.from(jwt));
  }

  /** 운영자가 신고를 처리합니다(기각·경고·가리기·이용 정지). */
  @PatchMapping("/api/admin/content-reports/{reportId}")
  public ReportDtos.Item handle(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long reportId,
      @Valid @RequestBody ReportDtos.HandleRequest request) {
    return service.handle(JwtMemberId.from(jwt), reportId, request);
  }
}
