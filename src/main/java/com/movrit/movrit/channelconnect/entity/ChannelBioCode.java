package com.movrit.movrit.channelconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소개글 인증 코드 하나입니다. 회원·서비스(인스타그램·네이버 블로그)마다 발급합니다.
 *
 * <ul>
 *   <li>24시간 동안 쓸 수 있고, 한 코드로 확인을 10번까지 시도할 수 있습니다.
 *   <li>확인에 성공하거나 운영자 확인으로 넘기면 '사용함'으로 바뀌어 다시 쓸 수 없습니다.
 * </ul>
 */
@Entity
@Table(name = "channel_bio_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelBioCode {
  /** 코드를 쓸 수 있는 시간입니다. 소개글을 고치는 데 시간이 걸릴 수 있어 넉넉히 둡니다. */
  public static final int VALID_HOURS = 24;

  /** 한 코드로 확인을 시도할 수 있는 횟수입니다. */
  public static final int MAX_CHECKS = 10;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ChannelProvider provider;

  @Column(nullable = false, length = 20)
  private String code;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "check_count", nullable = false)
  private int checkCount;

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public ChannelBioCode(Long memberId, ChannelProvider provider, String code) {
    this.memberId = memberId;
    this.provider = provider;
    this.code = code;
    this.createdAt = LocalDateTime.now();
    this.expiresAt = createdAt.plusHours(VALID_HOURS);
  }

  /** 아직 쓸 수 있는 코드인지 확인합니다(사용 전, 만료 전, 확인 횟수 남음). */
  public boolean isUsable(LocalDateTime now) {
    return usedAt == null && now.isBefore(expiresAt) && checkCount < MAX_CHECKS;
  }

  public int checksLeft() {
    return Math.max(0, MAX_CHECKS - checkCount);
  }

  /** 확인을 한 번 시도했음을 남깁니다. */
  public void recordCheck() {
    checkCount++;
  }

  /** 확인이 끝났거나 운영자 확인으로 넘겨 더 쓰지 않습니다. */
  public void markUsed() {
    this.usedAt = LocalDateTime.now();
  }
}
