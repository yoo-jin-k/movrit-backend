package com.movrit.movrit.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 신고입니다. 대상: 커뮤니티 글·댓글, 채팅 메시지, 캠페인 콘텐츠.
 *
 * <p>신고 당시 내용(target_snapshot)과 작성자(target_member_id)를 함께 남겨, 원문이 지워져도 운영자가 판단할 수 있게 합니다. 같은 사람이 같은
 * 대상을 두 번 신고할 수 없습니다. 상태: OPEN(접수) → RESOLVED(조치) 또는 DISMISSED(기각).
 */
@Entity
@Table(name = "content_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentReport {
  public static final String OPEN = "OPEN";
  public static final String RESOLVED = "RESOLVED";
  public static final String DISMISSED = "DISMISSED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reporter_member_id", nullable = false)
  private Long reporterMemberId;

  @Column(name = "target_type", nullable = false, length = 30)
  private String targetType;

  @Column(name = "target_id", nullable = false)
  private Long targetId;

  @Column(name = "target_member_id")
  private Long targetMemberId;

  @Column(name = "target_snapshot", length = 2000)
  private String targetSnapshot;

  @Column(nullable = false, length = 30)
  private String reason;

  @Column(length = 1000)
  private String detail;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(length = 30)
  private String resolution;

  @Column(name = "resolution_note", length = 500)
  private String resolutionNote;

  @Column(name = "handled_by_member_id")
  private Long handledByMemberId;

  @Column(name = "handled_at")
  private LocalDateTime handledAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public ContentReport(
      Long reporterMemberId,
      String targetType,
      Long targetId,
      Long targetMemberId,
      String targetSnapshot,
      String reason,
      String detail) {
    this.reporterMemberId = reporterMemberId;
    this.targetType = targetType;
    this.targetId = targetId;
    this.targetMemberId = targetMemberId;
    this.targetSnapshot = targetSnapshot;
    this.reason = reason;
    this.detail = detail;
    this.status = OPEN;
    this.createdAt = LocalDateTime.now();
  }

  public boolean isOpen() {
    return OPEN.equals(status);
  }

  /** 운영자 처리 결과를 기록합니다. resolution이 DISMISS면 기각, 그 밖에는 조치 완료입니다. */
  public void handle(String resolution, String note, Long adminMemberId) {
    if (!isOpen()) throw new IllegalStateException("이미 처리한 신고입니다.");
    this.status = "DISMISS".equals(resolution) ? DISMISSED : RESOLVED;
    this.resolution = resolution;
    this.resolutionNote = note;
    this.handledByMemberId = adminMemberId;
    this.handledAt = LocalDateTime.now();
  }
}
