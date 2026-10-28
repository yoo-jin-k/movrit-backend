package com.movrit.movrit.collaboration.controller;

import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.service.CampaignManagementService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 캠페인 및 협업의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collaborations/campaigns")
public class CampaignManagementController {
  private final CampaignManagementService service;

  /** 캠페인 및 협업 관련 캠페인 상세와 일정을 조회합니다. */
  @GetMapping("/{campaignId}")
  public CampaignManagementDtos.DetailResponse detail(@PathVariable Long campaignId) {
    return service.getDetail(campaignId);
  }

  /** 캠페인 및 협업 관련 수정을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PatchMapping("/{campaignId}")
  public CampaignManagementDtos.DetailResponse update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignManagementDtos.UpdateRequest request) {
    return service.update(memberId(jwt), campaignId, request);
  }

  /** 캠페인 및 협업 관련 종료 처리를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/{campaignId}/close")
  public CampaignManagementDtos.DetailResponse close(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.close(memberId(jwt), campaignId);
  }

  /** 소유 업체가 신청 당사자에게 공개할 캠페인 담당자 연락처를 저장합니다. */
  @PutMapping("/{campaignId}/contact")
  public CampaignManagementDtos.ContactResponse saveContact(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignManagementDtos.ContactSaveRequest request) {
    return service.saveContact(memberId(jwt), campaignId, request);
  }

  /** 소유 업체가 현재 등록된 캠페인 공개 담당자 연락처를 조회합니다. */
  @GetMapping("/{campaignId}/contact")
  public CampaignManagementDtos.ContactResponse getContact(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.getContact(memberId(jwt), campaignId);
  }

  /** 캠페인 및 협업 관련 삭제를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @DeleteMapping("/{campaignId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    service.delete(memberId(jwt), campaignId);
    return ResponseEntity.noContent().build();
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
