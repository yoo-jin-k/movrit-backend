package com.movrit.movrit.notification.dto;

import com.movrit.movrit.notification.entity.Notification;

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
    ) {
        public static Response from(Notification notification) {
            return new Response(
                    notification.getId(),
                    notification.getType(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getTargetType(),
                    notification.getTargetId(),
                    notification.isRead(),
                    notification.getReadAt(),
                    notification.getCreatedAt()
            );
        }
    }

    public record UnreadCount(long count) {}
}
