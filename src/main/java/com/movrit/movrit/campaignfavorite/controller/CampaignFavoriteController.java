package com.movrit.movrit.campaignfavorite.controller;

import com.movrit.movrit.campaignfavorite.dto.CampaignFavoriteDtos;
import com.movrit.movrit.campaignfavorite.service.CampaignFavoriteService;
import com.movrit.movrit.common.security.JwtMemberId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인한 회원의 캠페인 찜 API입니다.
 *
 * <ul>
 *   <li>GET /api/members/me/favorite-campaigns : 찜한 캠페인 목록
 *   <li>GET /api/members/me/favorite-campaigns/ids : 찜한 캠페인 ID 목록(카드 하트 표시용)
 *   <li>PUT /api/members/me/favorite-campaigns/{campaignId} : 찜하기(여러 번 호출해도 결과 동일)
 *   <li>DELETE /api/members/me/favorite-campaigns/{campaignId} : 찜 해제(여러 번 호출해도 결과 동일)
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/favorite-campaigns")
public class CampaignFavoriteController {
  private final CampaignFavoriteService service;

  @GetMapping
  public List<CampaignFavoriteDtos.Item> list(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(JwtMemberId.from(jwt));
  }

  @GetMapping("/ids")
  public List<Long> ids(@AuthenticationPrincipal Jwt jwt) {
    return service.findMyCampaignIds(JwtMemberId.from(jwt));
  }

  @PutMapping("/{campaignId}")
  public ResponseEntity<Void> add(@AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    service.add(JwtMemberId.from(jwt), campaignId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{campaignId}")
  public ResponseEntity<Void> remove(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    service.remove(JwtMemberId.from(jwt), campaignId);
    return ResponseEntity.noContent().build();
  }
}
