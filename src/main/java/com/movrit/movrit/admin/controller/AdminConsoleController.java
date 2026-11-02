package com.movrit.movrit.admin.controller;

import com.movrit.movrit.admin.dto.AdminConsoleDtos;
import com.movrit.movrit.admin.service.AdminConsoleService;
import com.movrit.movrit.admin.service.AdminReportService;
import com.movrit.movrit.common.security.JwtMemberId;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 관리 화면용 조회 API입니다(모두 운영자 전용·읽기 전용).
 *
 * <p>상태 변경은 기존 API를 씁니다: 회원 상태 PATCH /api/members/admin/{id}/status, 업체 승인·반려
 * /api/admin/sellers/{id}/approve·reject, 정산 확정·지급·취소 /api/settlements/{id}/...
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminConsoleController {
  private final AdminConsoleService service;
  private final AdminReportService reports;

  /** 회원 목록입니다. */
  @GetMapping("/members")
  public List<AdminConsoleDtos.MemberRow> members(@AuthenticationPrincipal Jwt jwt) {
    return service.members(JwtMemberId.from(jwt));
  }

  /** 회원 상세입니다. */
  @GetMapping("/members/{memberId}")
  public AdminConsoleDtos.MemberDetail member(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long memberId) {
    return service.member(JwtMemberId.from(jwt), memberId);
  }

  /** 업체 상세입니다. 업체 목록은 기존 GET /api/admin/sellers 를 씁니다. */
  @GetMapping("/sellers/{sellerId}")
  public AdminConsoleDtos.SellerDetail seller(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long sellerId) {
    return service.seller(JwtMemberId.from(jwt), sellerId);
  }

  /** 인플루언서 목록입니다. */
  @GetMapping("/creators")
  public List<AdminConsoleDtos.CreatorRow> creators(@AuthenticationPrincipal Jwt jwt) {
    return service.creators(JwtMemberId.from(jwt));
  }

  /** 인플루언서 상세입니다. */
  @GetMapping("/creators/{creatorId}")
  public AdminConsoleDtos.CreatorDetail creator(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long creatorId) {
    return service.creator(JwtMemberId.from(jwt), creatorId);
  }

  /** 프리오더 목록입니다. */
  @GetMapping("/preorders")
  public List<AdminConsoleDtos.PreorderRow> preorders(@AuthenticationPrincipal Jwt jwt) {
    return service.preorders(JwtMemberId.from(jwt));
  }

  /** 프리오더 상세입니다. */
  @GetMapping("/preorders/{preorderId}")
  public AdminConsoleDtos.PreorderDetail preorder(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long preorderId) {
    return service.preorder(JwtMemberId.from(jwt), preorderId);
  }

  /** 주문·환불 목록입니다. */
  @GetMapping("/orders")
  public List<AdminConsoleDtos.OrderRow> orders(@AuthenticationPrincipal Jwt jwt) {
    return service.orders(JwtMemberId.from(jwt));
  }

  /** 주문 상세입니다. */
  @GetMapping("/orders/{orderId}")
  public AdminConsoleDtos.OrderDetail order(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
    return service.order(JwtMemberId.from(jwt), orderId);
  }

  /** 크리에이터 협업 목록입니다. */
  @GetMapping("/collaborations")
  public List<AdminConsoleDtos.CollaborationRow> collaborations(@AuthenticationPrincipal Jwt jwt) {
    return service.collaborations(JwtMemberId.from(jwt));
  }

  /** 운영 리포트입니다. from·to(yyyy-MM-dd, 포함)를 비우면 전체 기간입니다. */
  @GetMapping("/reports")
  public AdminConsoleDtos.Report report(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return reports.report(JwtMemberId.from(jwt), from, to);
  }

  /** 운영 리포트 엑셀(.xlsx)입니다. 요약·날짜별·상태별 시트로 나뉩니다. */
  @GetMapping("/reports/export")
  public ResponseEntity<byte[]> exportReport(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    byte[] file = reports.export(JwtMemberId.from(jwt), from, to);
    String name =
        "MOVRIT_운영리포트_"
            + (from == null && to == null
                ? "전체"
                : (from == null ? "" : from) + "_" + (to == null ? "" : to))
            + ".xlsx";
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(name, StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(file);
  }

  /** 운영 설정 현황입니다. */
  @GetMapping("/settings")
  public AdminConsoleDtos.Settings settings(@AuthenticationPrincipal Jwt jwt) {
    return service.settings(JwtMemberId.from(jwt));
  }
}
