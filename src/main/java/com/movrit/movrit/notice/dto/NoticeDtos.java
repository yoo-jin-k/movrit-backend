package com.movrit.movrit.notice.dto;

import com.movrit.movrit.notice.entity.Notice;
import com.movrit.movrit.notice.entity.NoticeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 공지사항 API의 요청·응답 형식입니다. */
public final class NoticeDtos {
  private NoticeDtos() {}

  /** 운영자가 공지를 쓰거나 고칠 때 보내는 값입니다. */
  public record SaveRequest(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 20000) String body,
      @NotNull NoticeCategory category,
      boolean pinned,
      boolean published) {}

  /**
   * 공지 한 건입니다.
   *
   * @param authorName 작성 운영자 이름입니다. 사용자 화면에는 '운영팀'으로 표시합니다.
   */
  public record Response(
      Long id,
      String title,
      String body,
      NoticeCategory category,
      boolean pinned,
      boolean published,
      LocalDateTime publishedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String authorName) {
    /** 엔티티를 응답으로 바꿉니다. */
    public static Response from(Notice notice) {
      return new Response(
          notice.getId(),
          notice.getTitle(),
          notice.getBody(),
          notice.getCategory(),
          notice.isPinned(),
          notice.isPublished(),
          notice.getPublishedAt(),
          notice.getCreatedAt(),
          notice.getUpdatedAt(),
          notice.getAuthor() == null ? null : notice.getAuthor().getName());
    }
  }
}
