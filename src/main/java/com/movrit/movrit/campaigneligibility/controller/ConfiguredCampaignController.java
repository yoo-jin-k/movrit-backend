package com.movrit.movrit.campaigneligibility.controller;

import com.movrit.movrit.campaigneligibility.dto.ConfiguredCampaignDtos;
import com.movrit.movrit.campaigneligibility.service.ConfiguredCampaignService;
import com.movrit.movrit.common.security.JwtMemberId;
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

/** 캠페인 참여 조건의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns/configured")
public class ConfiguredCampaignController {
  private final ConfiguredCampaignService service;

  /**
   * 캠페인·신청서·선택한 참여 조건·이미지와 주소를 서비스의 단일 트랜잭션으로 생성합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저
   * 검사한 뒤 처리합니다.
   */
  @PostMapping
  public ResponseEntity<ConfiguredCampaignDtos.CreateResponse> create(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody ConfiguredCampaignDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(memberId(jwt), request));
  }

  /**
   * 캠페인 구성과 각 버전을 함께 전달합니다. 서비스가 버전 충돌을 검사하여 부분 저장을 방지합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을
   * 먼저 검사한 뒤 처리합니다.
   */
  @PatchMapping("/{campaignId}")
  public ConfiguredCampaignDtos.UpdateResponse update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody ConfiguredCampaignDtos.UpdateRequest request) {
    return service.update(memberId(jwt), campaignId, request);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
