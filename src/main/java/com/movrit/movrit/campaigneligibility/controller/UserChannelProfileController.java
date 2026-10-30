package com.movrit.movrit.campaigneligibility.controller;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.service.MemberChannelMetricService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 캠페인 참여 조건의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/channel-profile")
public class UserChannelProfileController {
  private final MemberChannelMetricService service;

  /** 로그인 회원에게 연결된 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping
  public CampaignEligibilityDtos.ChannelProfileResponse mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(memberId(jwt));
  }

  /** 저장을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PutMapping
  public CampaignEligibilityDtos.ChannelProfileResponse save(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody CampaignEligibilityDtos.MetricSaveRequest request) {
    return service.saveMine(memberId(jwt), request);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
