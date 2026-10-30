package com.movrit.movrit.emailverification.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** 이메일 인증 코드 발급 기록입니다. 코드 원문 대신 해시를 저장하고, 만료·사용·무효화 시각과 실패 횟수로 사용 가능 여부를 판단합니다. */
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

  /** 틀린 코드 입력을 1회 기록합니다. 최대 횟수에 도달하면 코드를 무효화해 더 이상 시도할 수 없게 합니다. */
  public void recordFailedAttempt(int maxAttempts) {
    attemptCount += 1;
    if (attemptCount >= maxAttempts) {
      invalidatedAt = LocalDateTime.now();
    }
  }

  /** 인증에 성공한 코드를 사용 완료로 표시합니다. 만료되었거나 이미 사용·무효화된 코드면 예외를 냅니다. */
  public void consume() {
    if (!isUsable(LocalDateTime.now())) {
      throw new IllegalStateException("만료되었거나 이미 사용된 인증 요청입니다.");
    }
    consumedAt = LocalDateTime.now();
  }

  /** 아직 사용되지 않은 코드를 무효화합니다. 새 코드를 보낼 때 이전 코드를 막는 데 씁니다. */
  public void invalidate() {
    if (consumedAt == null && invalidatedAt == null) {
      invalidatedAt = LocalDateTime.now();
    }
  }

  /** 지정한 시각 기준으로 사용·무효화되지 않았고 만료 전인 코드인지 확인합니다. */
  public boolean isUsable(LocalDateTime now) {
    return consumedAt == null && invalidatedAt == null && now.isBefore(expiresAt);
  }
}
