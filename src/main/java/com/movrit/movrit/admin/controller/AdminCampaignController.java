package com.movrit.movrit.admin.controller;

import com.movrit.movrit.admin.dto.AdminCampaignDtos;
import com.movrit.movrit.admin.service.AdminCampaignService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 캠페인 관리 API입니다. 운영자 권한은 서비스에서 확인합니다.
 *
 * <ul>
 *   <li>GET /api/admin/campaigns: 전체 캠페인과 신청 현황
 *   <li>PATCH /{id}/close: 사용자 목록에서 숨기기(마감, 사유 필수)
 *   <li>PATCH /{id}/reopen: 다시 공개
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/campaigns")
public class AdminCampaignController {
  private final AdminCampaignService service;

  @GetMapping
  public List<AdminCampaignDtos.Item> list(@AuthenticationPrincipal Jwt jwt) {
    return service.list(JwtMemberId.from(jwt));
  }

  @PatchMapping("/{campaignId}/close")
  public AdminCampaignDtos.Item close(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody AdminCampaignDtos.CloseRequest request) {
    return service.close(JwtMemberId.from(jwt), campaignId, request.reason());
  }

  @PatchMapping("/{campaignId}/reopen")
  public AdminCampaignDtos.Item reopen(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.reopen(JwtMemberId.from(jwt), campaignId);
  }
}
