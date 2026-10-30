package com.movrit.movrit.notification.entity;

import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** 회원에게 보내는 서비스 내부 알림입니다(선정 결과, 콘텐츠 검토 결과 등). 어떤 화면으로 이동할지 대상 종류·ID를 함께 저장합니다. */
@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String message;

  @Column(name = "target_type")
  private String targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /** 알림을 읽음으로 바꾸고 처음 읽은 시각을 기록합니다. 이미 읽은 알림은 그대로 둡니다. */
  public void markRead() {
    if (!read) {
      read = true;
      readAt = LocalDateTime.now();
    }
  }
}
