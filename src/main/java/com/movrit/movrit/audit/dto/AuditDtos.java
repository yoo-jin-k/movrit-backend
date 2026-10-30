package com.movrit.movrit.audit.dto;

import com.movrit.movrit.audit.entity.AuditLog;
import java.time.LocalDateTime;

/** 감사 이력 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
public final class AuditDtos {
  private AuditDtos() {}

  /**
   * 감사 이력의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param actorMemberId 작업을 수행한 회원 식별자이며, 수행자가 없으면 null입니다.
   * @param action 감사 대상 작업 코드입니다.
   * @param targetType 연결 대상의 유형입니다.
   * @param targetId 연결 대상의 식별자입니다.
   * @param beforeData 변경 전 감사 기록입니다.
   * @param afterData 변경 후 감사 기록입니다.
   * @param reason 처리 또는 판정 사유입니다.
   * @param createdAt 생성 시각입니다.
   */
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
    /** 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
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
