package com.movrit.movrit.campaigneligibility.controller;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.service.CampaignEligibilityPolicyService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns/{campaignId}/eligibility-requirement")
public class CampaignEligibilityRequirementController {
  private final CampaignEligibilityPolicyService service;

  @GetMapping
  public CampaignEligibilityDtos.RequirementResponse find(@PathVariable Long campaignId) {
    return service.find(campaignId);
  }

  @PutMapping
  public CampaignEligibilityDtos.RequirementResponse save(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignEligibilityDtos.RequirementSaveRequest request) {
    return service.save(memberId(jwt), campaignId, request);
  }

  @DeleteMapping
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @RequestParam Long expectedVersion) {
    service.delete(memberId(jwt), campaignId, expectedVersion);
    return ResponseEntity.noContent().build();
  }

  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
