package com.movrit.movrit.campaignmedia.controller;

import com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService;
import com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService.*;
import com.movrit.movrit.common.security.JwtMemberId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CampaignVisitEvidenceController {
  private final CampaignVisitEvidenceService service;

  @GetMapping("/api/campaign-applications/{applicationId}/visit-snapshot")
  public SnapshotView snapshot(@AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.snapshot(JwtMemberId.from(jwt), applicationId);
  }

  @GetMapping("/api/campaigns/{campaignId}/visit-history")
  public List<HistoryView> history(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.history(JwtMemberId.from(jwt), campaignId);
  }
}
