package com.movrit.movrit.phoneverification.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 휴대폰 인증번호 발급 기록입니다. 인증번호 원문 대신 해시를 저장하고, 만료·사용·무효화 시각과 틀린 입력 횟수로 사용 가능 여부를 판단합니다.
 *
 * <p>phone은 인증번호를 보낸 번호(숫자만)입니다. 인증에 성공하면 이 번호가 회원 연락처가 됩니다.
 */
@Entity
@Table(name = "phone_verification_challenge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PhoneVerificationChallenge extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(nullable = false, length = 20)
  private String phone;

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

  /** 틀린 인증번호 입력을 1회 기록합니다. 최대 횟수에 닿으면 무효화해 더 시도할 수 없게 합니다. */
  public void recordFailedAttempt(int maxAttempts) {
    attemptCount += 1;
    if (attemptCount >= maxAttempts) invalidatedAt = LocalDateTime.now();
  }

  /** 인증에 성공한 번호를 사용 완료로 표시합니다. 만료되었거나 이미 사용·무효화된 번호면 예외를 냅니다. */
  public void consume() {
    if (!isUsable(LocalDateTime.now())) {
      throw new IllegalStateException("만료되었거나 이미 사용된 인증 요청입니다.");
    }
    consumedAt = LocalDateTime.now();
  }

  /** 아직 사용하지 않은 인증번호를 무효화합니다. 새 번호를 보낼 때 이전 번호를 막는 데 씁니다. */
  public void invalidate() {
    if (consumedAt == null && invalidatedAt == null) invalidatedAt = LocalDateTime.now();
  }

  /** 사용·무효화되지 않았고 만료 전인지 확인합니다. */
  public boolean isUsable(LocalDateTime now) {
    return consumedAt == null && invalidatedAt == null && now.isBefore(expiresAt);
  }
}
