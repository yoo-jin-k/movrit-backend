package com.movrit.movrit.channelconnect.controller;

import com.movrit.movrit.channelconnect.dto.ChannelConnectDtos;
import com.movrit.movrit.channelconnect.service.ChannelConnectionService;
import com.movrit.movrit.channelconnect.service.ChannelEvidenceStorage;
import com.movrit.movrit.common.security.JwtMemberId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 '채널 확인' API입니다. 회원이 직접 등록한 SNS 채널(주소·팔로워 수·프로필 캡처)을 보고 확인하거나 반려합니다. 운영자 권한은 서비스에서 확인합니다.
 *
 * <ul>
 *   <li>GET /api/admin/channel-verifications: 직접 등록 목록(확인 대기 먼저)
 *   <li>GET /{id}/evidence: 프로필 캡처 이미지(저장하지 않도록 캐시 금지)
 *   <li>PATCH /{id}/approve: 확인
 *   <li>PATCH /{id}/reject: 반려(사유 필수)
 * </ul>
 */
@Tag(name = "운영자 · 채널 확인")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/channel-verifications")
public class AdminChannelVerificationController {
  private final ChannelConnectionService service;

  /** 운영자가 확인을 기다리는 직접 등록 채널 목록을 조회합니다. */
  @GetMapping
  public List<ChannelConnectDtos.ReviewItem> list(@AuthenticationPrincipal Jwt jwt) {
    return service.reviewList(JwtMemberId.from(jwt));
  }

  /** 운영자가 회원이 올린 채널 프로필 캡처 이미지를 내려받습니다. */
  @GetMapping("/{connectionId}/evidence")
  public ResponseEntity<Resource> evidence(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long connectionId) {
    Path path = service.evidence(JwtMemberId.from(jwt), connectionId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(
            MediaType.parseMediaType(
                ChannelEvidenceStorage.contentType(path.getFileName().toString())))
        .body(new FileSystemResource(path));
  }

  /** 운영자가 직접 등록 채널을 확인(승인)합니다. */
  @PatchMapping("/{connectionId}/approve")
  public ChannelConnectDtos.ReviewItem approve(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long connectionId) {
    return service.approve(JwtMemberId.from(jwt), connectionId);
  }

  /** 운영자가 직접 등록 채널을 반려하고 사유를 남깁니다. */
  @PatchMapping("/{connectionId}/reject")
  public ChannelConnectDtos.ReviewItem reject(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long connectionId,
      @Valid @RequestBody ChannelConnectDtos.RejectRequest request) {
    return service.reject(JwtMemberId.from(jwt), connectionId, request.reason());
  }
}
