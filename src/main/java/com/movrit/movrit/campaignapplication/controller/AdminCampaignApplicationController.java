package com.movrit.movrit.campaignapplication.controller;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.service.CampaignApplicationService;
import com.movrit.movrit.common.security.JwtMemberId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영자 선정 캠페인의 신청 목록과 선정 명령을 제공합니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/campaign-applications")
public class AdminCampaignApplicationController {
  private final CampaignApplicationService service;

  @GetMapping
  public List<CampaignApplicationDtos.Response> applications(
      @AuthenticationPrincipal Jwt jwt) {
    return service.findOperatorApplications(JwtMemberId.from(jwt));
  }

  @PatchMapping("/{applicationId}/select")
  public CampaignApplicationDtos.Response select(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.operatorSelect(JwtMemberId.from(jwt), applicationId);
  }

  @PatchMapping("/{applicationId}/reject")
  public CampaignApplicationDtos.Response reject(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.operatorReject(JwtMemberId.from(jwt), applicationId);
  }
}
