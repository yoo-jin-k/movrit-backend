package com.movrit.movrit.community.entity;

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

/** 커뮤니티 게시글입니다. 지우면 deleted_at만 기록하고, 운영자가 숨기면 hidden이 됩니다. */
@Entity
@Table(name = "community_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CommunityPost extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String board;

  @Column(nullable = false, length = 20)
  private String category;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, length = 5000)
  private String body;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_member_id")
  private Member author;

  @Column(name = "view_count", nullable = false)
  private int viewCount;

  @Column(name = "comment_count", nullable = false)
  private int commentCount;

  @Column(nullable = false)
  private boolean hidden;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** 목록·상세에 보여도 되는 글인지입니다. */
  public boolean visible() {
    return deletedAt == null && !hidden;
  }

  public void edit(String category, String title, String body) {
    this.category = category;
    this.title = title;
    this.body = body;
  }

  public void delete() {
    if (deletedAt == null) deletedAt = LocalDateTime.now();
  }

  public void hideByAdmin() {
    this.hidden = true;
  }

  public void increaseView() {
    viewCount++;
  }

  public void commentAdded() {
    commentCount++;
  }

  public void commentRemoved() {
    if (commentCount > 0) commentCount--;
  }
}
