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
 * 회원이 SNS 계정 로그인으로 연결한 채널 하나입니다.
 *
 * <p>접근 토큰은 저장하지 않고, 연결할 때 받은 계정 식별자·이름·주소·팔로워(구독자) 수만 보관합니다. 다시 연결하면 최신 값으로 덮어씁니다.
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

  @Column(name = "connected_at", nullable = false)
  private LocalDateTime connectedAt;

  /** 같은 서비스를 다시 연결했을 때 최신 계정 정보로 바꿉니다. */
  public void refresh(
      String externalId, String accountName, String profileUrl, Long followerCount) {
    this.externalId = externalId;
    this.accountName = accountName;
    this.profileUrl = profileUrl;
    this.followerCount = followerCount;
    this.connectedAt = LocalDateTime.now();
  }
}
