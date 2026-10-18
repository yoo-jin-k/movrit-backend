package com.movrit.movrit.notification.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.notification.dto.NotificationDtos;
import com.movrit.movrit.notification.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService service;

  @GetMapping
  public List<NotificationDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(memberId(jwt));
  }

  @GetMapping("/unread-count")
  public NotificationDtos.UnreadCount unreadCount(@AuthenticationPrincipal Jwt jwt) {
    return service.unreadCount(memberId(jwt));
  }

  @PatchMapping("/{notificationId}/read")
  public NotificationDtos.Response read(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long notificationId) {
    return service.markRead(memberId(jwt), notificationId);
  }

  @PatchMapping("/read-all")
  public ResponseEntity<Void> readAll(@AuthenticationPrincipal Jwt jwt) {
    service.markAllRead(memberId(jwt));
    return ResponseEntity.noContent().build();
  }

  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
