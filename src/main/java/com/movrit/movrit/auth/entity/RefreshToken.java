package com.movrit.movrit.auth.entity;

import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

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

  public void rotate(String hash, LocalDateTime expires) {
    tokenHash = hash;
    expiresAt = expires;
    revokedAt = null;
  }

  public void revoke() {
    revokedAt = LocalDateTime.now();
  }

  public boolean active() {
    return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
  }
}
