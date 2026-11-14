package com.movrit.movrit.seller.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.seller.dto.AdminSellerDtos;
import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.entity.SellerStatus;
import com.movrit.movrit.seller.service.AdminSellerQueryService;
import com.movrit.movrit.seller.service.SellerProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 업체의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다.
 *
 * <p>변경 이력: 전체 목록이 저장 순서 그대로·신청일 없이 내려가던 것 → 심사 상태(status)·정렬(sort) 조건과 신청일(createdAt)을 함께 주도록
 * 바꿨습니다. 정산 계좌 정보는 목록에서 빼고 상세 화면에서만 봅니다.
 */
@Tag(name = "운영자 · 업체 승인")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sellers")
public class AdminSellerController {

  private final SellerProfileService sellerProfileService;
  private final AdminSellerQueryService adminSellerQueryService;

  /**
   * 승인 및 반려 결과를 포함한 업체 신청 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다(운영자가 아니면 403).
   *
   * @param status 심사 상태(PENDING·APPROVED·REJECTED, 비우면 전체)
   * @param sort newest(신청일 최신순, 기본)·oldest(오래된순)·name(업체명순). 그 밖의 값은 400
   */
  @GetMapping
  public List<AdminSellerDtos.ListItem> findAll(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) SellerStatus status,
      @RequestParam(required = false) String sort) {
    return adminSellerQueryService.findForAdmin(memberId(jwt), status, sort);
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
