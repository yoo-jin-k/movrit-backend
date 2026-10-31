package com.movrit.movrit.notice.entity;

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
 * 운영자가 작성하는 공지사항 한 건입니다.
 *
 * <p>처음 게시할 때 게시 시각을 기록하고, 게시를 내려도 게시 시각은 남겨 둡니다(다시 게시하면 원래 순서를 유지합니다).
 */
@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notice extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NoticeCategory category;

  /** 목록 맨 위에 고정할지입니다. */
  @Column(nullable = false)
  private boolean pinned;

  /** 사용자에게 보이는지입니다. */
  @Column(nullable = false)
  private boolean published;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  /** 작성한 운영자입니다. 회원이 지워지면 비워집니다. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_member_id")
  private Member author;

  /** 제목·본문·구분·고정·게시 여부를 고칩니다. 처음 게시하는 순간에만 게시 시각을 기록합니다. */
  public void edit(
      String title, String body, NoticeCategory category, boolean pinned, boolean published) {
    this.title = title;
    this.body = body;
    this.category = category;
    this.pinned = pinned;
    this.published = published;
    if (published && publishedAt == null) publishedAt = LocalDateTime.now();
  }
}
