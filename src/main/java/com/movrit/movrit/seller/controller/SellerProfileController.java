package com.movrit.movrit.seller.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.service.SellerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 업체의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sellers")
public class SellerProfileController {

  private final SellerProfileService sellerProfileService;

  /**
   * JWT subject에는 로그인한 Member의 id가 들어 있습니다. 요청 body에서 memberId를 받지 않고 토큰에서 꺼내 사용해 본인 명의로만 신청할 수 있게
   * 합니다.
   *
   * <p>로그인 회원의 업체 등록 심사를 신청합니다. 요청 본문으로 신청자나 승인 상태를 지정하지 않습니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력
   * 형식을 먼저 검사한 뒤 처리합니다.
   */
  @PostMapping("/apply")
  public ResponseEntity<SellerDtos.Response> apply(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SellerDtos.ApplyRequest request) {
    Long memberId = JwtMemberId.from(jwt);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(sellerProfileService.apply(memberId, request));
  }

  /** 로그인 회원에게 연결된 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/me")
  public SellerDtos.Response findMine(@AuthenticationPrincipal Jwt jwt) {
    Long memberId = JwtMemberId.from(jwt);
    return sellerProfileService.findMine(memberId);
  }
}
