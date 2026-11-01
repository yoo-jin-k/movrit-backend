package com.movrit.movrit.channelconnect.controller;

import com.movrit.movrit.channelconnect.dto.ChannelConnectDtos;
import com.movrit.movrit.channelconnect.service.ChannelConnectionService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 회원의 SNS 채널(인스타그램·유튜브·네이버 블로그) 계정 연결 API입니다. 회원은 로그인 토큰으로만 확인합니다.
 *
 * <ul>
 *   <li>GET /api/users/me/channel-connections: 서비스별 연결 상태
 *   <li>POST /{provider}/start: SNS 로그인 주소 발급
 *   <li>POST /{provider}/complete: SNS에서 돌아온 인가 코드로 연결 완료
 *   <li>DELETE /{provider}: 연결 해제
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/channel-connections")
public class ChannelConnectionController {
  private final ChannelConnectionService service;

  @GetMapping
  public List<ChannelConnectDtos.ProviderStatus> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(JwtMemberId.from(jwt));
  }

  @PostMapping("/{provider}/start")
  public ChannelConnectDtos.StartResponse start(
      @AuthenticationPrincipal Jwt jwt, @PathVariable String provider) {
    return service.start(JwtMemberId.from(jwt), provider);
  }

  @PostMapping("/{provider}/complete")
  public ChannelConnectDtos.ProviderStatus complete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String provider,
      @Valid @RequestBody ChannelConnectDtos.CompleteRequest request) {
    return service.complete(JwtMemberId.from(jwt), provider, request);
  }

  @DeleteMapping("/{provider}")
  public ResponseEntity<Void> disconnect(
      @AuthenticationPrincipal Jwt jwt, @PathVariable String provider) {
    service.disconnect(JwtMemberId.from(jwt), provider);
    return ResponseEntity.noContent().build();
  }
}
