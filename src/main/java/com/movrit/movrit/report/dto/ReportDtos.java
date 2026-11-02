package com.movrit.movrit.report.dto;

import com.movrit.movrit.report.entity.ContentReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 신고·제재 API의 요청·응답 형식입니다. */
public final class ReportDtos {
  private ReportDtos() {}

  /**
   * 신고 요청입니다.
   *
   * @param targetType COMMUNITY_POST·COMMUNITY_COMMENT·CHAT_MESSAGE·CAMPAIGN_CONTENT
   * @param reason SPAM(광고·도배)·ABUSE(욕설·비방)·SEXUAL(음란)·PRIVACY(개인정보 노출)·FRAUD(사기·거짓)·OTHER(기타)
   */
  public record CreateRequest(
      @NotBlank String targetType,
      @NotNull Long targetId,
      @NotBlank String reason,
      @Size(max = 1000) String detail) {}

  /** 신고 접수 결과입니다(신고한 사람에게는 처리 결과만 알림으로 전달합니다). */
  public record Created(Long id, String status) {}

  /**
   * 운영자 처리 요청입니다.
   *
   * @param action DISMISS(기각)·WARN(경고 알림)·HIDE(대상 가리기)·SUSPEND(대상 가리기 + 작성자 이용 정지)
   */
  public record HandleRequest(@NotBlank String action, @Size(max = 500) String note) {}

  /** 운영자 목록 한 건입니다. */
  public record Item(
      Long id,
      String targetType,
      Long targetId,
      Long targetMemberId,
      String targetMemberName,
      String targetSnapshot,
      long sameTargetCount,
      Long reporterMemberId,
      String reporterName,
      String reason,
      String detail,
      String status,
      String resolution,
      String resolutionNote,
      LocalDateTime handledAt,
      LocalDateTime createdAt) {
    public static Item from(
        ContentReport report, String targetName, String reporterName, long sameTargetCount) {
      return new Item(
          report.getId(),
          report.getTargetType(),
          report.getTargetId(),
          report.getTargetMemberId(),
          targetName,
          report.getTargetSnapshot(),
          sameTargetCount,
          report.getReporterMemberId(),
          reporterName,
          report.getReason(),
          report.getDetail(),
          report.getStatus(),
          report.getResolution(),
          report.getResolutionNote(),
          report.getHandledAt(),
          report.getCreatedAt());
    }
  }
}
