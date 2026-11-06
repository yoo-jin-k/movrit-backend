package com.movrit.movrit.ai.controller;

import com.movrit.movrit.ai.dto.CampaignDraftDtos;
import com.movrit.movrit.ai.service.CampaignDraftService;
import com.movrit.movrit.common.security.JwtMemberId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 업체 캠페인 작성 화면의 AI 초안 API입니다. 권한·횟수 제한·AI 호출은 서비스가 맡습니다. */
@Tag(name = "업체 · AI 캠페인 초안")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns/ai")
public class CampaignDraftController {
  private final CampaignDraftService service;

  /**
   * 캠페인 소개·미션·제출 요건·주의사항 초안을 만듭니다. 승인된 업체 또는 운영자만 쓸 수 있고, 요청한 회원은 로그인 토큰으로 확인합니다. 초안은 저장하지 않습니다.
   *
   * <p>응답 상태: 비로그인 401, 제목 없음 400, 업체가 아님 403, 하루 횟수 초과 429, AI 응답 실패 503, AI 답 잘림·형식 오류 502.
   *
   * <p>변경 이력: 고치기 전에는 403·502 안내가 없었고 이 API를 직접 확인하는 자동 테스트가 없었습니다. 고친 후에는 응답 상태를 모두 적고, 로그인·입력 검사를
   * CampaignDraftControllerTest로 확인합니다.
   */
  @PostMapping("/draft")
  public CampaignDraftDtos.DraftResponse draft(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody CampaignDraftDtos.DraftRequest request) {
    return service.draft(JwtMemberId.from(jwt), request);
  }
}
