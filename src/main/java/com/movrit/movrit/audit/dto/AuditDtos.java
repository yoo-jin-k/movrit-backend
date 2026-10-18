package com.movrit.movrit.audit.dto;

import com.movrit.movrit.audit.entity.AuditLog;
import java.time.LocalDateTime;

public final class AuditDtos {
  private AuditDtos() {}

  public record Response(
      Long id,
      Long actorMemberId,
      String action,
      String targetType,
      Long targetId,
      String beforeData,
      String afterData,
      String reason,
      LocalDateTime createdAt) {
    public static Response from(AuditLog log) {
      return new Response(
          log.getId(),
          log.getActor() == null ? null : log.getActor().getId(),
          log.getAction(),
          log.getTargetType(),
          log.getTargetId(),
          log.getBeforeData(),
          log.getAfterData(),
          log.getReason(),
          log.getCreatedAt());
    }
  }
}
