package com.movrit.movrit.channelconnect.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 회원이 연결한 SNS 채널 하나입니다. 회원마다 서비스별로 하나만 둡니다.
 *
 * <ul>
 *   <li>로그인 연결(OAUTH): SNS가 알려준 계정 정보라 바로 '확인됨'입니다. 접근 토큰은 저장하지 않습니다.
 *   <li>직접 등록(MANUAL): 회원이 주소·팔로워 수·프로필 캡처를 올리고, 운영자가 확인하거나 반려합니다. 개인 인스타그램 계정처럼 로그인 연결이 안 되는 경우에
 *       씁니다.
 * </ul>
 */
@Entity
@Table(name = "member_channel_connection")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberChannelConnection extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ChannelProvider provider;

  /** SNS가 알려준 계정 고유 번호입니다(인스타그램 사용자 ID, 유튜브 채널 ID, 네이버 회원 식별자). */
  @Column(name = "external_id", nullable = false, length = 100)
  private String externalId;

  /** 화면에 보여줄 계정 이름입니다(인스타그램 @아이디, 유튜브 채널명, 네이버 블로그 아이디). */
  @Column(name = "account_name", length = 100)
  private String accountName;

  /** 채널 주소입니다. 브랜드가 참여자 채널을 열어볼 때 씁니다. */
  @Column(name = "profile_url", length = 500)
  private String profileUrl;

  /** 연결할 때 받은 팔로워(구독자) 수입니다. 서비스가 알려주지 않으면 null입니다. */
  @Column(name = "follower_count")
  private Long followerCount;

  /** 연결(또는 직접 등록)한 시각입니다. 직접 등록은 이 시각이 '확인 요청일'입니다. */
  @Column(name = "connected_at", nullable = false)
  private LocalDateTime connectedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "connection_method", nullable = false, length = 10)
  private ChannelConnectionMethod connectionMethod;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false, length = 20)
  private ChannelVerificationStatus verificationStatus;

  /** 직접 등록 때 올린 프로필 캡처의 저장 키입니다. 운영자만 볼 수 있습니다. */
  @Column(name = "evidence_key", length = 100)
  private String evidenceKey;

  /** 운영자가 반려할 때 남긴 사유입니다. 회원 화면에 그대로 보여줍니다. */
  @Column(name = "review_note", length = 500)
  private String reviewNote;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "reviewed_by_member_id")
  private Long reviewedByMemberId;

  /** 로그인으로 (다시) 연결했을 때 최신 계정 정보로 바꾸고 '확인됨'으로 둡니다. 예전 직접 등록 캡처 키는 비웁니다. */
  public void refresh(
      String externalId, String accountName, String profileUrl, Long followerCount) {
    this.externalId = externalId;
    this.accountName = accountName;
    this.profileUrl = profileUrl;
    this.followerCount = followerCount;
    this.connectedAt = LocalDateTime.now();
    this.connectionMethod = ChannelConnectionMethod.OAUTH;
    this.verificationStatus = ChannelVerificationStatus.VERIFIED;
    this.evidenceKey = null;
    clearReview();
  }

  /** 직접 등록(또는 다시 등록)합니다. 운영자 확인 전까지 '확인 대기'입니다. */
  public void registerManually(
      String externalId,
      String accountName,
      String profileUrl,
      Long followerCount,
      String evidenceKey) {
    this.externalId = externalId;
    this.accountName = accountName;
    this.profileUrl = profileUrl;
    this.followerCount = followerCount;
    this.evidenceKey = evidenceKey;
    this.connectedAt = LocalDateTime.now();
    this.connectionMethod = ChannelConnectionMethod.MANUAL;
    this.verificationStatus = ChannelVerificationStatus.PENDING;
    clearReview();
  }

  /** 운영자가 직접 등록한 채널을 확인합니다. 확인 대기 상태에서만 가능합니다. */
  public void approve(Long adminMemberId) {
    requirePendingManual();
    this.verificationStatus = ChannelVerificationStatus.VERIFIED;
    this.reviewNote = null;
    this.reviewedAt = LocalDateTime.now();
    this.reviewedByMemberId = adminMemberId;
  }

  /** 운영자가 직접 등록한 채널을 사유와 함께 반려합니다. 확인 대기 상태에서만 가능합니다. */
  public void reject(Long adminMemberId, String reason) {
    requirePendingManual();
    this.verificationStatus = ChannelVerificationStatus.REJECTED;
    this.reviewNote = reason;
    this.reviewedAt = LocalDateTime.now();
    this.reviewedByMemberId = adminMemberId;
  }

  private void requirePendingManual() {
    if (connectionMethod != ChannelConnectionMethod.MANUAL
        || verificationStatus != ChannelVerificationStatus.PENDING) {
      throw new IllegalStateException("확인 대기 중인 직접 등록 채널만 처리할 수 있습니다.");
    }
  }

  private void clearReview() {
    this.reviewNote = null;
    this.reviewedAt = null;
    this.reviewedByMemberId = null;
  }
}
