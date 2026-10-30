package com.movrit.movrit.collaboration.controller;

import com.movrit.movrit.collaboration.dto.CampaignPresentationDtos;
import com.movrit.movrit.collaboration.service.CampaignPresentationService;
import com.movrit.movrit.common.security.JwtMemberId;
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

/** 캠페인 및 협업의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns/{campaignId}/presentation")
public class CampaignPresentationController {
  private final CampaignPresentationService service;

  /** 표시 정보를 조회합니다. */
  @GetMapping
  public CampaignPresentationDtos.Response find(@PathVariable Long campaignId) {
    return service.find(campaignId);
  }

  /** 방문 장소명과 주소를 저장합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PutMapping("/location")
  public CampaignPresentationDtos.Response saveLocation(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignPresentationDtos.SaveRequest request) {
    return service.saveLocation(memberId(jwt), campaignId, request);
  }

  /** 캠페인 대표 이미지를 업로드합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PostMapping("/representative-image")
  public CampaignPresentationDtos.Response uploadRepresentative(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @RequestParam("file") MultipartFile file) {
    return service.uploadRepresentative(memberId(jwt), campaignId, file);
  }

  /** 캠페인 상세 이미지와 대체 설명을 업로드합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PostMapping("/detail-image")
  public CampaignPresentationDtos.Response uploadDetail(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "altText", required = false) String altText) {
    return service.uploadDetail(memberId(jwt), campaignId, file, altText);
  }

  /** 상세 이미지 표시 순서를 저장합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PutMapping("/detail-images/order")
  public CampaignPresentationDtos.Response reorderDetails(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignPresentationDtos.ReorderRequest request) {
    return service.reorderDetails(memberId(jwt), campaignId, request);
  }

  /** 대표 이미지 연결 제거를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @DeleteMapping("/representative-image")
  public CampaignPresentationDtos.Response clearRepresentative(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.clearRepresentative(memberId(jwt), campaignId);
  }

  /** 상세 이미지 연결 제거를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @DeleteMapping("/detail-image")
  public CampaignPresentationDtos.Response clearDetail(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.clearDetail(memberId(jwt), campaignId);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
