package com.movrit.movrit.campaignapplication.controller;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.dto.CampaignParticipantDtos;
import com.movrit.movrit.campaignapplication.service.CampaignApplicationService;
import com.movrit.movrit.campaignapplication.service.CampaignParticipantPortfolioService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 캠페인 신청의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns")
public class CampaignApplicationController {
  private final CampaignApplicationService service;
  private final CampaignParticipantPortfolioService participantPortfolioService;

  /**
   * 캠페인 신청 관련 캠페인 신청서의 질문·동의·참여 방식을 저장합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤
   * 처리합니다.
   */
  @PutMapping("/{campaignId}/application-form")
  public CampaignApplicationDtos.FormResponse saveForm(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignApplicationDtos.FormSaveRequest request) {
    return service.saveForm(memberId(jwt), campaignId, request);
  }

  /** 캠페인 신청 관련 캠페인 신청서 구성을 조회합니다. */
  @GetMapping("/{campaignId}/application-form")
  public CampaignApplicationDtos.FormResponse getForm(@PathVariable Long campaignId) {
    return service.getForm(campaignId);
  }

  /** 캠페인 신청 관련 현재 회원의 이메일 인증·모집 상태·중복 신청·캠페인 조건을 종합한 신청 가능 여부를 조회합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @GetMapping("/{campaignId}/eligibility")
  public CampaignApplicationDtos.EligibilityResponse eligibility(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.getEligibility(memberId(jwt), campaignId);
  }

  /**
   * 신청서 및 조건 버전과 답변을 전달합니다. 서비스가 모집·이메일·중복·조건을 재검증하고 신청 당시 정보를 보존합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청
   * 본문의 선언된 입력 제약을 검증한 뒤 처리합니다.
   */
  @PostMapping("/{campaignId}/applications")
  public ResponseEntity<CampaignApplicationDtos.Response> apply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignApplicationDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.apply(memberId(jwt), campaignId, request));
  }

  /** 캠페인 신청 관련 로그인 회원에게 연결된 정보를 조회합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @GetMapping("/applications/mine")
  public List<CampaignApplicationDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(memberId(jwt));
  }

  /** 마이페이지에서 신청 상태와 캠페인 일정·안내·콘텐츠 검수 상태를 한 번에 조회합니다. */
  @GetMapping("/applications/mine/workspaces")
  public List<CampaignApplicationDtos.WorkspaceResponse> myWorkspaces(
      @AuthenticationPrincipal Jwt jwt) {
    return service.findMyWorkspaces(memberId(jwt));
  }

  /** 캠페인 신청 관련 업체의 관리 대상 목록을 조회합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @GetMapping("/applications/seller")
  public List<CampaignApplicationDtos.Response> seller(@AuthenticationPrincipal Jwt jwt) {
    return service.findSellerApplications(memberId(jwt));
  }

  /** 업체가 소유한 캠페인의 신청자 활동 프로필과 완료 이력을 조회합니다. */
  @GetMapping("/applications/{applicationId}/seller-detail")
  public CampaignParticipantDtos.DetailResponse sellerDetail(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return participantPortfolioService.find(memberId(jwt), applicationId);
  }

  /**
   * 업체가 소유한 캠페인의 참여자 선정을 요청합니다. 서비스가 캠페인 행을 잠그고 선정·참여·콘텐츠 처리 중인 인원을 합산하여 모집 정원 초과를 방지합니다. 요청자의 회원
   * ID는 인증 토큰에서 가져옵니다.
   */
  @PatchMapping("/applications/{applicationId}/select")
  public CampaignApplicationDtos.Response select(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.select(memberId(jwt), applicationId);
  }

  /** 캠페인 신청 관련 반려 처리를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/applications/{applicationId}/reject")
  public CampaignApplicationDtos.Response reject(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.reject(memberId(jwt), applicationId);
  }

  /**
   * 선정된 신청의 당사자가 참여를 확정합니다. 서비스가 본인 신청을 확인하고 SELECTED에서 ACTIVE로 전환한 뒤 업체 알림과 감사 이력을 기록합니다. 요청자의 회원
   * ID는 인증 토큰에서 가져옵니다.
   */
  @PatchMapping("/applications/{applicationId}/activate")
  public CampaignApplicationDtos.Response activate(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.activate(memberId(jwt), applicationId);
  }

  /**
   * 신청 당사자의 취소 사유를 전달합니다. 서비스가 본인 신청과 취소 가능한 상태를 확인하고 알림 및 감사 이력을 기록합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다.
   * 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다.
   */
  @PatchMapping("/applications/{applicationId}/cancel")
  public CampaignApplicationDtos.Response cancel(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody CampaignApplicationDtos.CancelRequest request) {
    return service.cancel(memberId(jwt), applicationId, request);
  }

  /** 캠페인 신청 관련 참여자의 체험 완료 확인을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/applications/{applicationId}/experience-confirm")
  public CampaignApplicationDtos.Response confirmExperience(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.confirmExperience(memberId(jwt), applicationId);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
