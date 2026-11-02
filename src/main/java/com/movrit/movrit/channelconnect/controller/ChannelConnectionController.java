package com.movrit.movrit.channelconnect.controller;

import com.movrit.movrit.channelconnect.dto.ChannelConnectDtos;
import com.movrit.movrit.channelconnect.service.ChannelConnectionService;
import com.movrit.movrit.common.security.JwtMemberId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로그인 회원의 SNS 채널(인스타그램·유튜브·네이버 블로그) 계정 연결 API입니다. 회원은 로그인 토큰으로만 확인합니다.
 *
 * <ul>
 *   <li>GET /api/users/me/channel-connections: 서비스별 연결 상태
 *   <li>POST /{provider}/start: SNS 로그인 주소 발급
 *   <li>POST /{provider}/complete: SNS에서 돌아온 인가 코드로 연결 완료
 *   <li>POST /{provider}/manual: 직접 등록(주소·팔로워 수·프로필 캡처, multipart). 운영자 확인 전까지 '확인 대기'
 *   <li>DELETE /{provider}: 연결 해제
 * </ul>
 */
@Tag(name = "참여자 · SNS 채널 연결")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/channel-connections")
public class ChannelConnectionController {
  private final ChannelConnectionService service;

  /** 내 SNS 채널(인스타그램·유튜브·네이버 블로그)별 연결 상태를 조회합니다. */
  @GetMapping
  public List<ChannelConnectDtos.ProviderStatus> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(JwtMemberId.from(jwt));
  }

  /** SNS 로그인 연결을 시작하고 이동할 인증 주소를 받습니다. 로그인 연결이 꺼져 있으면 사용할 수 없습니다. */
  @PostMapping("/{provider}/start")
  public ChannelConnectDtos.StartResponse start(
      @AuthenticationPrincipal Jwt jwt, @PathVariable String provider) {
    return service.start(JwtMemberId.from(jwt), provider);
  }

  /** SNS 로그인에서 돌아온 인증 코드로 채널 연결을 마칩니다. */
  @PostMapping("/{provider}/complete")
  public ChannelConnectDtos.ProviderStatus complete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String provider,
      @Valid @RequestBody ChannelConnectDtos.CompleteRequest request) {
    return service.complete(JwtMemberId.from(jwt), provider, request);
  }

  /** 채널 주소·팔로워 수·프로필 캡처를 직접 등록해 운영자 확인을 요청합니다. */
  @PostMapping(value = "/{provider}/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ChannelConnectDtos.ProviderStatus registerManually(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String provider,
      @RequestParam String profileUrl,
      @RequestParam(required = false) Long followerCount,
      @RequestPart("evidence") MultipartFile evidence) {
    return service.registerManually(
        JwtMemberId.from(jwt), provider, profileUrl, followerCount, evidence);
  }

  /** 내 SNS 채널 연결을 해제합니다. */
  @DeleteMapping("/{provider}")
  public ResponseEntity<Void> disconnect(
      @AuthenticationPrincipal Jwt jwt, @PathVariable String provider) {
    service.disconnect(JwtMemberId.from(jwt), provider);
    return ResponseEntity.noContent().build();
  }
}
