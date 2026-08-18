package com.movrit.movrit.notification.dto;

import java.time.LocalDateTime;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record Response(
            Long id,
            String type,
            String title,
            String message,
            String targetType,
            Long targetId,
            boolean read,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {}

    public record UnreadCountResponse(long unreadCount) {}
}
