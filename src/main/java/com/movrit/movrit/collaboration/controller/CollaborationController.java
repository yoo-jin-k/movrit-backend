package com.movrit.movrit.collaboration.controller;

import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.service.CampaignSearchService;
import com.movrit.movrit.collaboration.service.CollaborationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 캠페인 및 협업의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@Tag(name = "공통 · 캠페인 목록·상세")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collaborations")
public class CollaborationController {
  private final CollaborationService s;
  private final CampaignSearchService search;

  /** 기존 캠페인 생성 경로를 처리합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping("/campaigns")
  public ResponseEntity<CollaborationDtos.CampaignResponse> campaign(
      @AuthenticationPrincipal Jwt j, @Valid @RequestBody CollaborationDtos.CampaignRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(s.createCampaign(id(j), r));
  }

  /** 현재 모집 중인 기존 캠페인 목록을 조회합니다. */
  @GetMapping("/campaigns/open")
  public List<CollaborationDtos.CampaignResponse> open() {
    return s.openCampaigns();
  }

  /**
   * 사용자 캠페인 목록·검색입니다. 서버에서 조건으로 거르고 정렬해 페이지로 나눠 줍니다.
   *
   * <p>예: /campaigns/search?q=앰플&type=PRODUCT&channel=INSTAGRAM&sort=closing&page=0&size=20
   */
  @GetMapping("/campaigns/search")
  public CollaborationDtos.CampaignPage search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String channel,
      @RequestParam(defaultValue = "false") boolean premier,
      @RequestParam(defaultValue = "false") boolean openedToday,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return search.search(
        new CampaignSearchService.Query(
            q, type, category, region, channel, premier, openedToday, sort, page, size));
  }

  /** 업체가 관리하는 캠페인 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/campaigns/seller")
  public List<CollaborationDtos.CampaignResponse> sellerCampaigns(@AuthenticationPrincipal Jwt j) {
    return s.sellerCampaigns(id(j));
  }

  /** 참여자가 캠페인 협업을 신청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping("/campaigns/{campaignId}/apply")
  public CollaborationDtos.Response apply(
      @AuthenticationPrincipal Jwt j,
      @PathVariable Long campaignId,
      @Valid @RequestBody CollaborationDtos.ApplyRequest r) {
    return s.apply(id(j), campaignId, r);
  }

  /** 채널 프로필을 대상으로 협업을 제안합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping("/proposals")
  public CollaborationDtos.Response propose(
      @AuthenticationPrincipal Jwt j, @Valid @RequestBody CollaborationDtos.ProposeRequest r) {
    return s.propose(id(j), r);
  }

  /** 로그인 회원에게 연결된 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/mine")
  public List<CollaborationDtos.Response> mine(@AuthenticationPrincipal Jwt j) {
    return s.mine(id(j));
  }

  /** 업체의 관리 대상 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/seller")
  public List<CollaborationDtos.Response> seller(@AuthenticationPrincipal Jwt j) {
    return s.seller(id(j));
  }

  /** 승인 처리를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{id}/approve")
  public CollaborationDtos.Response approve(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.sellerDecision(id(j), id, true);
  }

  /** 반려 처리를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{id}/reject")
  public CollaborationDtos.Response reject(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.sellerDecision(id(j), id, false);
  }

  /** 협업 제안 수락을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{id}/accept")
  public CollaborationDtos.Response accept(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.creatorDecision(id(j), id, true);
  }

  /** 협업 제안 거절을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{id}/decline")
  public CollaborationDtos.Response decline(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.creatorDecision(id(j), id, false);
  }

  /** 협업 종료를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{id}/end")
  public CollaborationDtos.Response end(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.end(id(j), id);
  }

  /** 추천 링크 방문 정보를 기록합니다. */
  @PostMapping("/referrals/{code}/click")
  public ResponseEntity<Void> click(
      @PathVariable String code, @RequestBody CollaborationDtos.ClickRequest r) {
    s.click(code, r);
    return ResponseEntity.noContent().build();
  }

  /** 로그인 참여자의 추천 성과를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/performance/mine")
  public List<CollaborationDtos.PerformanceResponse> myPerformance(@AuthenticationPrincipal Jwt j) {
    return s.creatorPerformance(id(j));
  }

  /** 업체 관점의 추천 성과를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/performance/seller")
  public List<CollaborationDtos.PerformanceResponse> sellerPerformance(
      @AuthenticationPrincipal Jwt j) {
    return s.sellerPerformance(id(j));
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long id(Jwt j) {
    return Long.valueOf(j.getSubject());
  }
}
