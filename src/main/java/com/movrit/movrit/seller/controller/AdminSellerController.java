package com.movrit.movrit.seller.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 업체의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sellers")
public class AdminSellerController {

  private final SellerProfileService sellerProfileService;

  /** 승인 및 반려 결과를 포함한 전체 업체 신청 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping
  public List<SellerDtos.Response> findAll(@AuthenticationPrincipal Jwt jwt) {
    return sellerProfileService.findAllForAdmin(memberId(jwt));
  }

  /** 관리자용 업체 등록 심사 대기 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/pending")
  public List<SellerDtos.Response> findPending(@AuthenticationPrincipal Jwt jwt) {
    return sellerProfileService.findPendingForAdmin(memberId(jwt));
  }

  /** 관리자가 업체 신청을 승인합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{sellerId}/approve")
  public SellerDtos.Response approve(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long sellerId) {
    return sellerProfileService.approve(memberId(jwt), sellerId);
  }

  /** 관리자가 업체 신청을 반려합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{sellerId}/reject")
  public SellerDtos.Response reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long sellerId) {
    return sellerProfileService.reject(memberId(jwt), sellerId);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
