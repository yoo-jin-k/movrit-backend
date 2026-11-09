package com.movrit.movrit.ai.controller;

import com.movrit.movrit.ai.dto.ContentPrecheckDtos;
import com.movrit.movrit.ai.service.ContentPrecheckService;
import com.movrit.movrit.common.security.JwtMemberId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참여자 콘텐츠 제출 화면의 제출 전 점검 API입니다. 권한·단계·횟수 제한·AI 호출은 서비스가 맡습니다. */
@Tag(name = "참여자 · 제출 전 콘텐츠 점검")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaign-contents/ai")
public class ContentPrecheckController {
  private final ContentPrecheckService service;

  /**
   * 붙여 넣은 본문이 캠페인 미션·제출 요건·주의사항과 협찬 표시 기준을 지켰는지 점검합니다. 신청 당사자만 쓸 수 있고, 요청한 회원은 로그인 토큰으로 확인합니다. 본문과
   * 결과는 저장하지 않으며 제출 자체는 기존 제출 API로 따로 합니다.
   *
   * <p>응답 상태: 비로그인 401, 본문 없음·8,000자 초과 400, 본인 신청 아님 404, 점검할 수 없는 단계 409, 하루 횟수 초과 429, AI 응답 실패
   * 503, AI 답 잘림·형식 오류 502.
   */
  @PostMapping("/applications/{applicationId}/precheck")
  public ContentPrecheckDtos.PrecheckResponse precheck(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody ContentPrecheckDtos.PrecheckRequest request) {
    return service.precheck(JwtMemberId.from(jwt), applicationId, request);
  }
}
