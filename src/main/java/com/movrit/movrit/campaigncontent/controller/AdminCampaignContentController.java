package com.movrit.movrit.campaigncontent.controller;

import com.movrit.movrit.campaigncontent.dto.CampaignContentDtos;
import com.movrit.movrit.campaigncontent.service.CampaignContentService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/campaign-contents")
public class AdminCampaignContentController {
  private final CampaignContentService service;

  @GetMapping
  public List<CampaignContentDtos.Response> findAll(@AuthenticationPrincipal Jwt jwt) {
    return service.findOperatorContents(memberId(jwt));
  }

  @PatchMapping("/{submissionId}/request-revision")
  public CampaignContentDtos.Response requestRevision(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody CampaignContentDtos.ReviewRequest request) {
    return service.requestRevisionByOperator(memberId(jwt), submissionId, request);
  }

  @PatchMapping("/{submissionId}/approve")
  public CampaignContentDtos.Response approve(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody CampaignContentDtos.ApproveRequest request) {
    return service.approveByOperator(memberId(jwt), submissionId, request);
  }

  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
