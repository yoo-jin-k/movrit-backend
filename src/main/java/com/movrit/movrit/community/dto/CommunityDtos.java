package com.movrit.movrit.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** 커뮤니티 게시판 요청·응답 형식입니다. */
public final class CommunityDtos {
  private CommunityDtos() {}

  /** 목록의 글 한 줄입니다. preview는 본문 앞부분입니다. */
  public record PostSummary(
      Long id,
      String board,
      String category,
      String title,
      String preview,
      String authorName,
      int viewCount,
      int commentCount,
      LocalDateTime createdAt) {}

  /** 목록 한 페이지입니다(20개씩). */
  public record PostPage(List<PostSummary> items, int page, int totalPages, long total) {}

  /**
   * 댓글입니다. mine은 로그인한 내가 쓴 댓글인지, deletable은 내가 지울 수 있는지(본인·운영자)입니다. hidden이면 운영자가 가린 댓글이라 body에 안내
   * 문구가 들어갑니다.
   */
  public record Comment(
      Long id,
      String authorName,
      String body,
      LocalDateTime createdAt,
      boolean mine,
      boolean deletable,
      boolean hidden) {}

  // ───────── 운영자 커뮤니티 관리 ─────────

  /** 운영자 목록의 글 한 줄입니다. status는 VISIBLE·HIDDEN·DELETED 중 하나입니다. */
  public record AdminPostSummary(
      Long id,
      String board,
      String category,
      String title,
      String preview,
      Long authorMemberId,
      String authorName,
      int viewCount,
      int commentCount,
      long hiddenCommentCount,
      String status,
      LocalDateTime hiddenAt,
      LocalDateTime createdAt) {}

  /** 운영자 목록 한 페이지입니다. */
  public record AdminPostPage(List<AdminPostSummary> items, int page, int totalPages, long total) {}

  /** 운영자에게 보이는 댓글입니다. 가린 댓글·지운 댓글도 원문 그대로 보여 줍니다. */
  public record AdminComment(
      Long id,
      Long authorMemberId,
      String authorName,
      String body,
      String status,
      LocalDateTime hiddenAt,
      LocalDateTime createdAt) {}

  /** 운영자에게 보이는 글 상세입니다. */
  public record AdminPostDetail(AdminPostSummary post, String body, List<AdminComment> comments) {}

  /** 가리기·다시 보이기 요청입니다. note는 작성자 알림과 감사 기록에 남깁니다. */
  public record VisibilityRequest(boolean hidden, @Size(max = 300) String note) {}

  /** 글 상세입니다. */
  public record PostDetail(
      Long id,
      String board,
      String category,
      String title,
      String body,
      String authorName,
      int viewCount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      boolean mine,
      boolean deletable,
      List<Comment> comments) {}

  /** 글 쓰기·고치기 요청입니다. */
  public record PostRequest(
      @NotBlank @Size(max = 20) String board,
      @NotBlank @Size(max = 20) String category,
      @NotBlank @Size(min = 2, max = 100) String title,
      @NotBlank @Size(min = 5, max = 5000) String body) {}

  /** 댓글 쓰기 요청입니다. */
  public record CommentRequest(@NotBlank @Size(max = 1000) String body) {}
}
