package com.movrit.movrit.campaigncontent.controller;

import com.movrit.movrit.campaigncontent.dto.CampaignContentDtos;
import com.movrit.movrit.campaigncontent.service.CampaignContentService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 제출 콘텐츠의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/campaign-contents")
public class AdminCampaignContentController {
  private final CampaignContentService service;

  /** 관리 대상 전체 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping
  public List<CampaignContentDtos.Response> findAll(@AuthenticationPrincipal Jwt jwt) {
    return service.findOperatorContents(memberId(jwt));
  }

  /**
   * 운영자 권한으로 콘텐츠 수정 의견을 전달합니다. 서비스가 권한·버전·현재 상태를 검사하고 참여자에게 내부 알림을 보냅니다. 요청한 회원은 로그인 토큰으로 확인합니다.
   * 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. 수정·재제출 시 expectedVersion은 서비스의 충돌 검사에 사용되므로 최신 조회값을 전달해야 합니다.
   */
  @PatchMapping("/{submissionId}/request-revision")
  public CampaignContentDtos.Response requestRevision(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody CampaignContentDtos.ReviewRequest request) {
    return service.requestRevisionByOperator(memberId(jwt), submissionId, request);
  }

  /**
   * 운영자 권한으로 콘텐츠 최종 검토를 요청합니다. 서비스가 권한·버전을 검사하고 콘텐츠 승인과 신청 완료 상태를 함께 반영합니다. 요청한 회원은 로그인 토큰으로 확인합니다.
   * 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. 수정·재제출 시 expectedVersion은 서비스의 충돌 검사에 사용되므로 최신 조회값을 전달해야 합니다.
   */
  @PatchMapping("/{submissionId}/approve")
  public CampaignContentDtos.Response approve(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody CampaignContentDtos.ApproveRequest request) {
    return service.approveByOperator(memberId(jwt), submissionId, request);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
