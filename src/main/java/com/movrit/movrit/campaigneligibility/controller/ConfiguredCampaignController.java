package com.movrit.movrit.campaigneligibility.controller;

import com.movrit.movrit.campaigneligibility.dto.ConfiguredCampaignDtos;
import com.movrit.movrit.campaigneligibility.service.ConfiguredCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns/configured")
public class ConfiguredCampaignController {
  private final ConfiguredCampaignService service;

  @PostMapping
  public ResponseEntity<ConfiguredCampaignDtos.CreateResponse> create(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody ConfiguredCampaignDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(memberId(jwt), request));
  }

  @PatchMapping("/{campaignId}")
  public ConfiguredCampaignDtos.UpdateResponse update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody ConfiguredCampaignDtos.UpdateRequest request) {
    return service.update(memberId(jwt), campaignId, request);
  }

  private Long memberId(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
