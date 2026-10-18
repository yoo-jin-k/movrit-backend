package com.movrit.movrit.creator.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

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

  public void update(
      String nickname, String bio, String primaryCategory, String snsUrl, Long followerCount) {
    this.nickname = nickname;
    this.bio = bio;
    this.primaryCategory = primaryCategory;
    this.snsUrl = snsUrl;
    this.followerCount = followerCount;
  }
}
