package com.movrit.movrit.campaignshipping.controller;

import com.movrit.movrit.campaignshipping.dto.CampaignShippingDtos;
import com.movrit.movrit.campaignshipping.service.CampaignShippingService;
import com.movrit.movrit.common.security.JwtMemberId;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "공통 · 캠페인 배송")
@RestController
@RequiredArgsConstructor
public class CampaignShippingController {
  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final CampaignShippingService service;

  /** 배송 등록에 쓸 수 있는 택배사 목록을 조회합니다. */
  @GetMapping("/api/campaigns/shipping/couriers")
  public List<String> couriers() {
    return CampaignShippingService.COURIERS;
  }

  /** 캠페인 업체가 선정자별 배송지·송장·수령 확인 목록을 조회합니다. */
  @GetMapping("/api/campaigns/{campaignId}/shipping")
  public List<CampaignShippingDtos.Row> rows(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.rows(JwtMemberId.from(jwt), campaignId);
  }

  /** 캠페인 업체가 송장 여러 건을 한 번에 저장하고 건별 결과를 돌려받습니다. */
  @PutMapping("/api/campaigns/{campaignId}/shipping")
  public CampaignShippingDtos.SaveResponse save(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long campaignId,
      @Valid @RequestBody CampaignShippingDtos.SaveRequest request) {
    return service.save(JwtMemberId.from(jwt), campaignId, request);
  }

  /** 캠페인 업체가 배송지 목록을 엑셀(.xlsx) 파일로 내려받습니다. */
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

  /** 참여자·업체가 신청 건의 발송 정보(택배사·송장 번호)를 조회합니다. 발송 전이면 204를 돌려줍니다. */
  @GetMapping("/api/campaigns/applications/{applicationId}/shipment")
  public ResponseEntity<CampaignShippingDtos.Shipment> shipment(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service
        .shipment(JwtMemberId.from(jwt), applicationId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }
}
