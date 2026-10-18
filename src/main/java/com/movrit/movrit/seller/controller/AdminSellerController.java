package com.movrit.movrit.seller.controller;

import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sellers")
public class AdminSellerController {

  private final SellerProfileService sellerProfileService;

  /** 승인/반려 결과까지 포함한 전체 판매자 신청 목록 조회 */
  @GetMapping
  public List<SellerDtos.Response> findAll(@AuthenticationPrincipal Jwt jwt) {
    return sellerProfileService.findAllForAdmin(memberId(jwt));
  }

  /** 관리자용 판매자 등록 심사 대기 목록 조회 */
  @GetMapping("/pending")
  public List<SellerDtos.Response> findPending(@AuthenticationPrincipal Jwt jwt) {
    return sellerProfileService.findPendingForAdmin(memberId(jwt));
  }

  /** 관리자 승인 처리 */
  @PatchMapping("/{sellerId}/approve")
  public SellerDtos.Response approve(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long sellerId) {
    return sellerProfileService.approve(memberId(jwt), sellerId);
  }

  /** 관리자 반려 처리 */
  @PatchMapping("/{sellerId}/reject")
  public SellerDtos.Response reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long sellerId) {
    return sellerProfileService.reject(memberId(jwt), sellerId);
  }

  private Long memberId(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
