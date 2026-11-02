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

  /** 댓글입니다. mine은 로그인한 내가 쓴 댓글인지, deletable은 내가 지울 수 있는지(본인·운영자)입니다. */
  public record Comment(
      Long id,
      String authorName,
      String body,
      LocalDateTime createdAt,
      boolean mine,
      boolean deletable) {}

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
