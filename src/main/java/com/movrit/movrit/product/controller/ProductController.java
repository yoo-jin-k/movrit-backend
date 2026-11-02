package com.movrit.movrit.product.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.product.dto.ProductDtos;
import com.movrit.movrit.product.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

/** 상품 및 옵션의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@Tag(name = "이전 기능 · 상품")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  /** 상품을 등록합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping
  public ResponseEntity<ProductDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProductDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(productService.create(memberId(jwt), request));
  }

  /** 로그인 회원에게 연결된 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/mine")
  public List<ProductDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
    return productService.findMine(memberId(jwt));
  }

  /** 식별자로 상세 정보를 조회합니다. */
  @GetMapping("/{id}")
  public ProductDtos.Response findById(@PathVariable Long id) {
    return productService.findById(id);
  }

  /** 상품을 고칩니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PutMapping("/{id}")
  public ProductDtos.Response update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @Valid @RequestBody ProductDtos.UpdateRequest request) {
    return productService.update(memberId(jwt), id, request);
  }

  /** 상품 대표 이미지를 업로드합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PostMapping("/{id}/thumbnail")
  public ProductDtos.Response uploadThumbnail(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @RequestParam("file") MultipartFile file) {
    return productService.uploadThumbnail(memberId(jwt), id, file);
  }

  /** 상품 옵션을 추가합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping("/{id}/variants")
  public ProductDtos.VariantResponse createVariant(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @Valid @RequestBody ProductDtos.VariantCreateRequest request) {
    return productService.createVariant(memberId(jwt), id, request);
  }

  /** 상품 옵션 목록을 조회합니다. */
  @GetMapping("/{id}/variants")
  public List<ProductDtos.VariantResponse> findVariants(@PathVariable Long id) {
    return productService.findVariants(id);
  }

  /** 옵션 재고 입고를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PatchMapping("/variants/{variantId}/restock")
  public ProductDtos.VariantResponse restock(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long variantId,
      @Valid @RequestBody ProductDtos.StockRequest request) {
    return productService.restock(memberId(jwt), variantId, request.quantity());
  }

  /** 상품 옵션의 안전 재고를 고칩니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PatchMapping("/variants/{variantId}/safety-stock")
  public ProductDtos.VariantResponse updateSafetyStock(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long variantId,
      @Valid @RequestBody ProductDtos.SafetyStockRequest request) {
    return productService.updateSafetyStock(memberId(jwt), variantId, request.quantity());
  }

  /** 상품 옵션을 고칩니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PutMapping("/variants/{variantId}")
  public ProductDtos.VariantResponse updateVariant(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long variantId,
      @Valid @RequestBody ProductDtos.VariantUpdateRequest request) {
    return productService.updateVariant(memberId(jwt), variantId, request);
  }

  /** 옵션 비활성화를 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @DeleteMapping("/variants/{variantId}")
  public ProductDtos.VariantResponse deactivateVariant(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long variantId) {
    return productService.deactivateVariant(memberId(jwt), variantId);
  }

  /** 상품을 지웁니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    productService.delete(memberId(jwt), id);
    return ResponseEntity.noContent().build();
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
