package com.movrit.movrit.notification.dto;

import com.movrit.movrit.notification.entity.Notification;
import java.time.LocalDateTime;

/** 내부 알림의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class NotificationDtos {
  private NotificationDtos() {}

  /**
   * 내부 알림의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param type 데이터 또는 질문 유형입니다.
   * @param title 제목입니다.
   * @param message 신청 또는 안내 메시지입니다.
   * @param targetType 연결 대상의 유형입니다.
   * @param targetId 연결 대상의 식별자입니다.
   * @param read 알림 읽음 여부입니다.
   * @param readAt 알림을 읽은 시각입니다.
   * @param createdAt 생성 시각입니다.
   */
  public record Response(
      Long id,
      String type,
      String title,
      String message,
      String targetType,
      Long targetId,
      boolean read,
      LocalDateTime readAt,
      LocalDateTime createdAt) {
    /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
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
          notification.getCreatedAt());
    }
  }

  /**
   * 내부 알림의 읽지 않은 알림 건수를 전달합니다.
   *
   * @param count 집계 건수입니다.
   */
  public record UnreadCount(long count) {}
}
