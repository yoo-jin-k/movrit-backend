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

/** 게시글 댓글입니다. 지우면 deleted_at만 기록합니다. */
@Entity
@Table(name = "community_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CommunityComment extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "post_id", nullable = false)
  private CommunityPost post;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_member_id")
  private Member author;

  @Column(nullable = false, length = 1000)
  private String body;

  /** 운영자가 가린 댓글입니다. 자리는 남기고 내용 대신 안내 문구를 보여 줍니다. */
  @Column(nullable = false)
  private boolean hidden;

  @Column(name = "hidden_at")
  private LocalDateTime hiddenAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void delete() {
    if (deletedAt == null) deletedAt = LocalDateTime.now();
  }

  public void hideByAdmin() {
    if (!hidden) hiddenAt = LocalDateTime.now();
    hidden = true;
  }

  public void unhideByAdmin() {
    hidden = false;
    hiddenAt = null;
  }
}
