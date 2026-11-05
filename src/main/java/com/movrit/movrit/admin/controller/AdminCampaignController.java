package com.movrit.movrit.admin.controller;

import com.movrit.movrit.admin.dto.AdminCampaignDtos;
import com.movrit.movrit.admin.service.AdminCampaignService;
import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.common.security.JwtMemberId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 캠페인 관리 API입니다. 운영자 권한은 서비스에서 확인합니다.
 *
 * <ul>
 *   <li>GET /api/admin/campaigns: 전체 캠페인과 신청 현황
 *   <li>PATCH /{id}/close: 사용자 목록에서 숨기기(마감, 사유 필수)
 *   <li>PATCH /{id}/reopen: 다시 공개
 *   <li>GET·PUT·DELETE /{id}/eligibility-requirement: 신청 조건 조회·설정·해제
 * </ul>
 */
@Tag(name = "운영자 · 캠페인")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/campaigns")
public class AdminCampaignController {
  private final AdminCampaignService service;

  /** 운영자가 전체 캠페인 목록을 조회합니다. */
  @GetMapping
  public List<AdminCampaignDtos.Item> list(@AuthenticationPrincipal Jwt jwt) {
    return service.list(JwtMemberId.from(jwt));
  }

  /** 운영자가 캠페인 모집을 마감합니다. */
  @PatchMapping("/{campaignId}/close")
  public AdminCampaignDtos.Item close(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody AdminCampaignDtos.CloseRequest request) {
    return service.close(JwtMemberId.from(jwt), campaignId, request.reason());
  }

  /** 운영자가 마감된 캠페인 모집을 다시 엽니다. */
  @PatchMapping("/{campaignId}/reopen")
  public AdminCampaignDtos.Item reopen(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.reopen(JwtMemberId.from(jwt), campaignId);
  }

  /** 운영자가 캠페인의 현재 신청 조건을 조회합니다. 조건이 없으면 빈 응답(null)입니다. */
  @GetMapping("/{campaignId}/eligibility-requirement")
  public CampaignEligibilityDtos.RequirementResponse requirement(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.requirement(JwtMemberId.from(jwt), campaignId);
  }

  /** 운영자가 캠페인 신청 조건(인스타그램 팔로워 수 또는 네이버 블로그 일평균 방문자)을 정하거나 고칩니다. 입력 형식을 먼저 검사하고, 버전이 다르면 409입니다. */
  @PutMapping("/{campaignId}/eligibility-requirement")
  public CampaignEligibilityDtos.RequirementResponse saveRequirement(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignEligibilityDtos.RequirementSaveRequest request) {
    return service.saveRequirement(JwtMemberId.from(jwt), campaignId, request);
  }

  /** 운영자가 캠페인 신청 조건을 해제합니다. 마지막으로 본 버전과 다르면 409입니다. */
  @DeleteMapping("/{campaignId}/eligibility-requirement")
  public ResponseEntity<Void> deleteRequirement(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @RequestParam Long expectedVersion) {
    service.deleteRequirement(JwtMemberId.from(jwt), campaignId, expectedVersion);
    return ResponseEntity.noContent().build();
  }
}
