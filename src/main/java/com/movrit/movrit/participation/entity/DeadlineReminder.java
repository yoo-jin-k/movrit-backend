package com.movrit.movrit.participation.entity;

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

/** 콘텐츠 마감 알림 발송 기록입니다. 같은 신청·단계(D3·D1)로는 한 번만 보냅니다. */
@Entity
@Table(name = "campaign_deadline_reminder")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeadlineReminder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  @Column(nullable = false, length = 10)
  private String stage;

  @Column(name = "sent_at", nullable = false)
  private LocalDateTime sentAt;

  public DeadlineReminder(Long applicationId, String stage) {
    this.applicationId = applicationId;
    this.stage = stage;
    this.sentAt = LocalDateTime.now();
  }
}
