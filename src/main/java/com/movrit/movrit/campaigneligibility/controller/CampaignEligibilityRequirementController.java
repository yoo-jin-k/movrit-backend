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

/** 캠페인 참여 조건의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns/{campaignId}/eligibility-requirement")
public class CampaignEligibilityRequirementController {
  private final CampaignEligibilityPolicyService service;

  /** 캠페인 참여 조건 관련 표시 정보를 조회합니다. */
  @GetMapping
  public CampaignEligibilityDtos.RequirementResponse find(@PathVariable Long campaignId) {
    return service.find(campaignId);
  }

  /**
   * 업체가 해당 캠페인의 채널·지표·최소값을 지정합니다. 모든 캠페인에 고정 수치를 적용하지 않습니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된
   * 입력 제약을 검증한 뒤 처리합니다.
   */
  @PutMapping
  public CampaignEligibilityDtos.RequirementResponse save(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignEligibilityDtos.RequirementSaveRequest request) {
    return service.save(memberId(jwt), campaignId, request);
  }

  /** 기존 조건의 버전을 확인하여 해당 캠페인의 지표 조건을 해제하도록 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @DeleteMapping
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @RequestParam Long expectedVersion) {
    service.delete(memberId(jwt), campaignId, expectedVersion);
    return ResponseEntity.noContent().build();
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
