package com.movrit.movrit.campaigncontent.controller;

import com.movrit.movrit.campaigncontent.dto.CampaignContentDtos;
import com.movrit.movrit.campaigncontent.service.CampaignContentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaign-contents")
public class CampaignContentController {
  private final CampaignContentService service;

  @PostMapping("/applications/{applicationId}")
  public ResponseEntity<CampaignContentDtos.Response> submit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody CampaignContentDtos.SubmitRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.submit(memberId(jwt), applicationId, request));
  }

  @GetMapping("/mine")
  public List<CampaignContentDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(memberId(jwt));
  }

  @GetMapping("/seller")
  public List<CampaignContentDtos.Response> seller(@AuthenticationPrincipal Jwt jwt) {
    return service.findSellerContents(memberId(jwt));
  }

  @PatchMapping("/{submissionId}/request-revision")
  public CampaignContentDtos.Response requestRevision(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody CampaignContentDtos.ReviewRequest request) {
    return service.requestRevision(memberId(jwt), submissionId, request);
  }

  @PatchMapping("/{submissionId}/approve")
  public CampaignContentDtos.Response approve(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody CampaignContentDtos.ApproveRequest request) {
    return service.approve(memberId(jwt), submissionId, request);
  }

  private Long memberId(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
