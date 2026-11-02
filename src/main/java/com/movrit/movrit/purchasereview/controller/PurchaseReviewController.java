package com.movrit.movrit.purchasereview.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.purchasereview.dto.PurchaseReviewDtos;
import com.movrit.movrit.purchasereview.service.PurchaseReviewService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 구매평 캠페인 API입니다.
 *
 * <ul>
 *   <li>GET /api/collaborations/campaigns/{campaignId}/purchase-review: 구매 안내(공개)
 *   <li>PUT /api/campaigns/{campaignId}/purchase-review: 구매 안내 저장(캠페인 업체)
 *   <li>POST /api/campaigns/applications/{applicationId}/purchase-proof: 구매 인증 제출(참여자, multipart:
 *       orderNumber·paidAmount·evidence)
 *   <li>GET /api/campaigns/applications/{applicationId}/purchase-proof: 구매 인증·환급 상태(참여자·업체·운영자, 없으면
 *       204)
 *   <li>GET /api/campaigns/applications/{applicationId}/purchase-proof/evidence: 주문 내역 캡처
 *   <li>PATCH /api/campaigns/applications/{applicationId}/payback: 환급 완료 기록(캠페인 업체)
 *   <li>GET /api/campaigns/purchase-paybacks: 업체 환급 관리 목록
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class PurchaseReviewController {
  private final PurchaseReviewService service;

  @GetMapping("/api/collaborations/campaigns/{campaignId}/purchase-review")
  public PurchaseReviewDtos.Info info(@PathVariable Long campaignId) {
    return service.info(campaignId);
  }

  @PutMapping("/api/campaigns/{campaignId}/purchase-review")
  public PurchaseReviewDtos.Info saveInfo(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody PurchaseReviewDtos.InfoRequest request) {
    return service.saveInfo(JwtMemberId.from(jwt), campaignId, request);
  }

  @PostMapping(
      value = "/api/campaigns/applications/{applicationId}/purchase-proof",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public PurchaseReviewDtos.Proof submitProof(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @RequestParam("orderNumber") String orderNumber,
      @RequestParam("paidAmount") Integer paidAmount,
      @RequestPart("evidence") MultipartFile evidence) {
    return service.submitProof(
        JwtMemberId.from(jwt), applicationId, orderNumber, paidAmount, evidence);
  }

  @GetMapping("/api/campaigns/applications/{applicationId}/purchase-proof")
  public ResponseEntity<PurchaseReviewDtos.Proof> proof(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service
        .proof(JwtMemberId.from(jwt), applicationId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @GetMapping("/api/campaigns/applications/{applicationId}/purchase-proof/evidence")
  public ResponseEntity<Resource> evidence(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    Path path = service.evidence(JwtMemberId.from(jwt), applicationId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(PurchaseReviewService.evidenceType(path)))
        .body(new FileSystemResource(path));
  }

  /** 업체 '환급 관리' 목록입니다(구매평 캠페인 참여자별 구매 인증·환급 상태). */
  @GetMapping("/api/campaigns/purchase-paybacks")
  public java.util.List<PurchaseReviewDtos.PaybackRow> paybacks(@AuthenticationPrincipal Jwt jwt) {
    return service.sellerPaybacks(JwtMemberId.from(jwt));
  }

  @PatchMapping("/api/campaigns/applications/{applicationId}/payback")
  public PurchaseReviewDtos.Proof markPaid(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody(required = false) PurchaseReviewDtos.PaybackRequest request) {
    return service.markPaid(JwtMemberId.from(jwt), applicationId, request);
  }
}
