package com.movrit.movrit.contentmetric.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.contentmetric.dto.ContentMetricDtos;
import com.movrit.movrit.contentmetric.service.ContentMetricService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "공통 · 콘텐츠 성과")
@RestController
@RequiredArgsConstructor
public class ContentMetricController {
  private final ContentMetricService service;

  /** 제출 콘텐츠의 날짜별 성과(조회수·좋아요·댓글) 기록을 조회합니다. */
  @GetMapping("/api/campaign-contents/{submissionId}/metrics")
  public ContentMetricDtos.SubmissionMetrics read(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long submissionId) {
    return service.read(JwtMemberId.from(jwt), submissionId);
  }

  /** 참여자 또는 캠페인 업체가 특정 날짜의 콘텐츠 성과를 기록하거나 고칩니다. */
  @PutMapping("/api/campaign-contents/{submissionId}/metrics")
  public ContentMetricDtos.SubmissionMetrics record(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody ContentMetricDtos.RecordRequest request) {
    return service.record(JwtMemberId.from(jwt), submissionId, request);
  }

  /** 캠페인 업체가 캠페인 전체 콘텐츠의 성과 합계와 콘텐츠별 최신 값을 조회합니다. */
  @GetMapping("/api/campaigns/{campaignId}/content-performance")
  public ContentMetricDtos.CampaignPerformance campaign(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.campaign(JwtMemberId.from(jwt), campaignId);
  }
}
