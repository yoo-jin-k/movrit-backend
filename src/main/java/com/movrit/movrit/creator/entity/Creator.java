package com.movrit.movrit.creator.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캠페인 참여자의 활동 프로필입니다(테이블 influencer_profile). 신청할 때 자동으로 만들어지며, 회원 한 명당 하나만 가집니다. 별도 인플루언서 가입은
 * 없습니다.
 */
@Entity
@Table(name = "influencer_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Creator extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false, unique = true)
  private Member member;

  @Column(name = "nickname", nullable = false)
  private String nickname;

  @Column(name = "bio", columnDefinition = "TEXT")
  private String bio;

  @Column(name = "primary_category")
  private String primaryCategory;

  @Column(name = "sns_url", length = 500)
  private String snsUrl;

  @Column(name = "follower_count", nullable = false)
  private Long followerCount;

  @Column(name = "status", nullable = false)
  private String status;

  /** 활동 프로필의 표시 이름·소개·주요 분야·SNS 주소·팔로워 수를 한 번에 수정합니다. */
  public void update(
      String nickname, String bio, String primaryCategory, String snsUrl, Long followerCount) {
    this.nickname = nickname;
    this.bio = bio;
    this.primaryCategory = primaryCategory;
    this.snsUrl = snsUrl;
    this.followerCount = followerCount;
  }
}
