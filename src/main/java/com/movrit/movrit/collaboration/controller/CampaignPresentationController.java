package com.movrit.movrit.collaboration.controller;

import com.movrit.movrit.collaboration.dto.CampaignPresentationDtos;
import com.movrit.movrit.collaboration.service.CampaignPresentationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns/{campaignId}/presentation")
public class CampaignPresentationController {
  private final CampaignPresentationService service;

  @GetMapping
  public CampaignPresentationDtos.Response find(@PathVariable Long campaignId) {
    return service.find(campaignId);
  }

  @PutMapping("/location")
  public CampaignPresentationDtos.Response saveLocation(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignPresentationDtos.SaveRequest request) {
    return service.saveLocation(memberId(jwt), campaignId, request);
  }

  @PostMapping("/representative-image")
  public CampaignPresentationDtos.Response uploadRepresentative(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @RequestParam("file") MultipartFile file) {
    return service.uploadRepresentative(memberId(jwt), campaignId, file);
  }

  @PostMapping("/detail-image")
  public CampaignPresentationDtos.Response uploadDetail(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "altText", required = false) String altText) {
    return service.uploadDetail(memberId(jwt), campaignId, file, altText);
  }

  @PutMapping("/detail-images/order")
  public CampaignPresentationDtos.Response reorderDetails(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignPresentationDtos.ReorderRequest request) {
    return service.reorderDetails(memberId(jwt), campaignId, request);
  }

  @DeleteMapping("/representative-image")
  public CampaignPresentationDtos.Response clearRepresentative(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.clearRepresentative(memberId(jwt), campaignId);
  }

  @DeleteMapping("/detail-image")
  public CampaignPresentationDtos.Response clearDetail(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.clearDetail(memberId(jwt), campaignId);
  }

  private Long memberId(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
