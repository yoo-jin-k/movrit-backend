package com.movrit.movrit.notification.entity;

import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

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

  public void markRead() {
    if (!read) {
      read = true;
      readAt = LocalDateTime.now();
    }
  }
}
