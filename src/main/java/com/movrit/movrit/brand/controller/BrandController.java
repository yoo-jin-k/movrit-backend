package com.movrit.movrit.brand.controller;

import com.movrit.movrit.brand.dto.BrandDtos;
import com.movrit.movrit.brand.entity.BrandStatus;
import com.movrit.movrit.brand.service.BrandService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 브랜드의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/brands")
public class BrandController {

  private final BrandService brandService;

  /** 생성을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping
  public BrandDtos.Response create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BrandDtos.CreateRequest request) {
    return brandService.create(memberId(jwt), request);
  }

  /** 로그인 회원에게 연결된 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/mine")
  public List<BrandDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
    return brandService.findMine(memberId(jwt));
  }

  /** 수정을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PutMapping("/{brandId}")
  public BrandDtos.Response update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long brandId,
      @Valid @RequestBody BrandDtos.UpdateRequest request) {
    return brandService.update(memberId(jwt), brandId, request);
  }

  /** 브랜드 로고 파일을 업로드합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PostMapping("/{brandId}/logo")
  public BrandDtos.Response uploadLogo(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long brandId,
      @RequestParam("file") MultipartFile file) {
    return brandService.uploadLogo(memberId(jwt), brandId, file);
  }

  /** 브랜드 커버 파일을 업로드합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PostMapping("/{brandId}/cover")
  public BrandDtos.Response uploadCover(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long brandId,
      @RequestParam("file") MultipartFile file) {
    return brandService.uploadCover(memberId(jwt), brandId, file);
  }

  /** 브랜드 상태 변경을 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{brandId}/status")
  public BrandDtos.Response changeStatus(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long brandId,
      @RequestParam BrandStatus status) {
    return brandService.changeStatus(memberId(jwt), brandId, status);
  }

  /** 삭제를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @DeleteMapping("/{brandId}")
  public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long brandId) {
    brandService.delete(memberId(jwt), brandId);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
