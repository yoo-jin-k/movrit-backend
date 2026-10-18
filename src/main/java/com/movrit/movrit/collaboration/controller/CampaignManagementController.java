package com.movrit.movrit.collaboration.controller;

import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.service.CampaignManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collaborations/campaigns")
public class CampaignManagementController {
  private final CampaignManagementService service;

  @GetMapping("/{campaignId}")
  public CampaignManagementDtos.DetailResponse detail(@PathVariable Long campaignId) {
    return service.getDetail(campaignId);
  }

  @PatchMapping("/{campaignId}")
  public CampaignManagementDtos.DetailResponse update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignManagementDtos.UpdateRequest request) {
    return service.update(memberId(jwt), campaignId, request);
  }

  @PatchMapping("/{campaignId}/close")
  public CampaignManagementDtos.DetailResponse close(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.close(memberId(jwt), campaignId);
  }

  @DeleteMapping("/{campaignId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    service.delete(memberId(jwt), campaignId);
    return ResponseEntity.noContent().build();
  }

  private Long memberId(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
