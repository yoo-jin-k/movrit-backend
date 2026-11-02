package com.movrit.movrit.campaigncontent.controller;

import com.movrit.movrit.campaigncontent.dto.CampaignContentDtos;
import com.movrit.movrit.campaigncontent.service.CampaignContentService;
import com.movrit.movrit.common.security.JwtMemberId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 제출 콘텐츠의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@Tag(name = "공통 · 콘텐츠 제출·검수")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaign-contents")
public class CampaignContentController {
  private final CampaignContentService service;

  /**
   * 신청 당사자가 콘텐츠 URL을 제출합니다. 최초 제출은 신규 기록을 만들고, 재제출은 기존 제출의 버전과 상태를 검사합니다. 요청한 회원은 로그인 토큰으로 확인합니다.
   * 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. 수정·재제출 시 expectedVersion은 서비스의 충돌 검사에 사용되므로 최신 조회값을 전달해야 합니다.
   */
  @PostMapping("/applications/{applicationId}")
  public ResponseEntity<CampaignContentDtos.Response> submit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody CampaignContentDtos.SubmitRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.submit(memberId(jwt), applicationId, request));
  }

  /** 신청 당사자가 채널별 게시물 URL을 함께 제출하거나 수정 요청 후 재제출합니다. */
  @PostMapping("/applications/{applicationId}/links")
  public ResponseEntity<CampaignContentDtos.Response> submitLinks(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody CampaignContentDtos.PackageSubmitRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.submitLinks(memberId(jwt), applicationId, request));
  }

  /** 로그인 회원에게 연결된 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/mine")
  public List<CampaignContentDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(memberId(jwt));
  }

  /** 업체의 관리 대상 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/seller")
  public List<CampaignContentDtos.Response> seller(@AuthenticationPrincipal Jwt jwt) {
    return service.findSellerContents(memberId(jwt));
  }

  /**
   * 소유 업체가 콘텐츠 수정 의견을 전달합니다. 서비스가 제출과 신청 상태를 함께 수정 요청 상태로 전환하고 참여자에게 내부 알림을 보냅니다. 요청한 회원은 로그인 토큰으로
   * 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. 수정·재제출 시 expectedVersion은 서비스의 충돌 검사에 사용되므로 최신 조회값을
   * 전달해야 합니다.
   */
  @PatchMapping("/{submissionId}/request-revision")
  public CampaignContentDtos.Response requestRevision(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody CampaignContentDtos.ReviewRequest request) {
    return service.requestRevision(memberId(jwt), submissionId, request);
  }

  /**
   * 소유 업체의 콘텐츠 검토 완료를 요청합니다. 이 경로는 BRAND_APPROVED 처리이며 운영자 최종 승인과 구분합니다. 요청한 회원은 로그인 토큰으로 확인합니다.
   * 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. 수정·재제출 시 expectedVersion은 서비스의 충돌 검사에 사용되므로 최신 조회값을 전달해야 합니다.
   */
  @PatchMapping("/{submissionId}/approve")
  public CampaignContentDtos.Response approve(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long submissionId,
      @Valid @RequestBody CampaignContentDtos.ApproveRequest request) {
    return service.approve(memberId(jwt), submissionId, request);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
