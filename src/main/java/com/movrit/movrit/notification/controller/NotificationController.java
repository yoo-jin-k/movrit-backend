package com.movrit.movrit.notification.controller;

import com.movrit.movrit.notification.dto.NotificationDtos;
import com.movrit.movrit.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.findMine(memberId(jwt));
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.unreadCount(memberId(jwt));
    }

    @PatchMapping("/{id}/read")
    public NotificationDtos.Response markRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return notificationService.markRead(memberId(jwt), id);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllRead(memberId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
