package com.movrit.movrit.emailverification.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "email_verification_challenge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailVerificationChallenge extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "email_snapshot", nullable = false, length = 100)
  private String emailSnapshot;

  @Column(name = "code_hash", nullable = false, length = 100)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "consumed_at")
  private LocalDateTime consumedAt;

  @Column(name = "invalidated_at")
  private LocalDateTime invalidatedAt;

  @Column(name = "attempt_count", nullable = false)
  private Integer attemptCount;

  public void recordFailedAttempt(int maxAttempts) {
    attemptCount += 1;
    if (attemptCount >= maxAttempts) {
      invalidatedAt = LocalDateTime.now();
    }
  }

  public void consume() {
    if (!isUsable(LocalDateTime.now())) {
      throw new IllegalStateException("만료되었거나 이미 사용된 인증 요청입니다.");
    }
    consumedAt = LocalDateTime.now();
  }

  public void invalidate() {
    if (consumedAt == null && invalidatedAt == null) {
      invalidatedAt = LocalDateTime.now();
    }
  }

  public boolean isUsable(LocalDateTime now) {
    return consumedAt == null && invalidatedAt == null && now.isBefore(expiresAt);
  }
}
