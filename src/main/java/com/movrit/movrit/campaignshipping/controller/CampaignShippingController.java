package com.movrit.movrit.campaignshipping.controller;

import com.movrit.movrit.campaignshipping.dto.CampaignShippingDtos;
import com.movrit.movrit.campaignshipping.service.CampaignShippingService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배송 관리 API입니다.
 *
 * <ul>
 *   <li>GET /api/campaigns/{campaignId}/shipping: 업체 배송 관리 목록(배송지·송장·수령 확인)
 *   <li>PUT /api/campaigns/{campaignId}/shipping: 송장 여러 건 저장(건별 결과)
 *   <li>GET /api/campaigns/{campaignId}/shipping/export: 배송지 엑셀(.xlsx) 내려받기
 *   <li>GET /api/campaigns/shipping/couriers: 고를 수 있는 택배사
 *   <li>GET /api/campaigns/applications/{applicationId}/shipment: 참여자·업체용 발송 정보(없으면 204)
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class CampaignShippingController {
  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final CampaignShippingService service;

  @GetMapping("/api/campaigns/shipping/couriers")
  public List<String> couriers() {
    return CampaignShippingService.COURIERS;
  }

  @GetMapping("/api/campaigns/{campaignId}/shipping")
  public List<CampaignShippingDtos.Row> rows(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.rows(JwtMemberId.from(jwt), campaignId);
  }

  @PutMapping("/api/campaigns/{campaignId}/shipping")
  public CampaignShippingDtos.SaveResponse save(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignShippingDtos.SaveRequest request) {
    return service.save(JwtMemberId.from(jwt), campaignId, request);
  }

  @GetMapping("/api/campaigns/{campaignId}/shipping/export")
  public ResponseEntity<byte[]> export(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    byte[] file = service.export(JwtMemberId.from(jwt), campaignId);
    String name = "배송지_캠페인" + campaignId + "_" + LocalDate.now() + ".xlsx";
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(XLSX)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(name, StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(file);
  }

  @GetMapping("/api/campaigns/applications/{applicationId}/shipment")
  public ResponseEntity<CampaignShippingDtos.Shipment> shipment(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service
        .shipment(JwtMemberId.from(jwt), applicationId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }
}
