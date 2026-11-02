package com.movrit.movrit.notification.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.notification.dto.NotificationDtos;
import com.movrit.movrit.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 내부 알림의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@Tag(name = "공통 · 알림")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService service;

  /** 로그인 회원에게 연결된 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping
  public List<NotificationDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(memberId(jwt));
  }

  /** 읽지 않은 내부 알림 건수를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/unread-count")
  public NotificationDtos.UnreadCount unreadCount(@AuthenticationPrincipal Jwt jwt) {
    return service.unreadCount(memberId(jwt));
  }

  /** 지정한 내부 알림을 읽음 처리합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/{notificationId}/read")
  public NotificationDtos.Response read(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long notificationId) {
    return service.markRead(memberId(jwt), notificationId);
  }

  /** 로그인 회원의 내부 알림을 모두 읽음 처리합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PatchMapping("/read-all")
  public ResponseEntity<Void> readAll(@AuthenticationPrincipal Jwt jwt) {
    service.markAllRead(memberId(jwt));
    return ResponseEntity.noContent().build();
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
