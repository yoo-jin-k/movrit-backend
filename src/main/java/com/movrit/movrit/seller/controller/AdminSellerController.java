package com.movrit.movrit.seller.controller;

import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.service.SellerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sellers")
public class AdminSellerController {

    private final SellerProfileService sellerProfileService;

    /** 관리자용 판매자 등록 심사 대기 목록 조회 */
    @GetMapping("/pending")
    public List<SellerDtos.Response> findPending(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return sellerProfileService.findPendingForAdmin(memberId(jwt));
    }

    /** 관리자 승인 처리 */
    @PatchMapping("/{sellerId}/approve")
    public SellerDtos.Response approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sellerId
    ) {
        return sellerProfileService.approve(memberId(jwt), sellerId);
    }

    /** 관리자 반려 처리 */
    @PatchMapping("/{sellerId}/reject")
    public SellerDtos.Response reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sellerId
    ) {
        return sellerProfileService.reject(memberId(jwt), sellerId);
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
