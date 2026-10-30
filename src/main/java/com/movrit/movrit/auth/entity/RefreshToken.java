package com.movrit.movrit.auth.entity;

import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** 로그인 유지용 리프레시 토큰입니다. 회원당 하나만 두며, 토큰 원문 대신 해시값과 만료·폐기 시각만 저장합니다. */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false, unique = true)
  private Member member;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  /** 토큰을 새로 발급할 때 해시값과 만료 시각을 바꾸고 폐기 표시를 지웁니다. 이전 토큰은 더 이상 맞지 않게 됩니다. */
  public void rotate(String hash, LocalDateTime expires) {
    tokenHash = hash;
    expiresAt = expires;
    revokedAt = null;
  }

  /** 로그아웃 등으로 토큰을 즉시 폐기합니다. */
  public void revoke() {
    revokedAt = LocalDateTime.now();
  }

  /** 폐기되지 않았고 만료 전인 토큰인지 확인합니다. */
  public boolean active() {
    return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
  }
}
