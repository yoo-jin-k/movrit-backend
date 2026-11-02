package com.movrit.movrit.contentmetric.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.contentmetric.dto.ContentMetricDtos;
import com.movrit.movrit.contentmetric.service.ContentMetricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 콘텐츠 성과 API입니다.
 *
 * <ul>
 *   <li>GET /api/campaign-contents/{submissionId}/metrics: 게시물별 최근 기록(참여자·캠페인 업체·운영자)
 *   <li>PUT /api/campaign-contents/{submissionId}/metrics: 하루치 조회수·좋아요·댓글 기록(참여자·캠페인 업체)
 *   <li>GET /api/campaigns/{campaignId}/content-performance: 캠페인 성과 요약(캠페인 업체·운영자)
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class ContentMetricController {
  private final ContentMetricService service;

  @GetMapping("/api/campaign-contents/{submissionId}/metrics")
  public ContentMetricDtos.SubmissionMetrics read(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long submissionId) {
    return service.read(JwtMemberId.from(jwt), submissionId);
  }

  @PutMapping("/api/campaign-contents/{submissionId}/metrics")
  public ContentMetricDtos.SubmissionMetrics record(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody ContentMetricDtos.RecordRequest request) {
    return service.record(JwtMemberId.from(jwt), submissionId, request);
  }

  @GetMapping("/api/campaigns/{campaignId}/content-performance")
  public ContentMetricDtos.CampaignPerformance campaign(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.campaign(JwtMemberId.from(jwt), campaignId);
  }
}
