package com.movrit.movrit.campaignmedia.controller;

import com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService;
import com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService.*;
import com.movrit.movrit.common.security.JwtMemberId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 캠페인 이미지와 방문 주소의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
public class CampaignVisitEvidenceController {
  private final CampaignVisitEvidenceService service;

  /** 신청 당시 보존한 방문 주소 사본을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/api/campaign-applications/{applicationId}/visit-snapshot")
  public SnapshotView snapshot(@AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.snapshot(JwtMemberId.from(jwt), applicationId);
  }

  /** 캠페인 방문 주소 변경 이력을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/api/campaigns/{campaignId}/visit-history")
  public List<HistoryView> history(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.history(JwtMemberId.from(jwt), campaignId);
  }
}
