package com.movrit.movrit.community.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.community.dto.CommunityDtos;
import com.movrit.movrit.community.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커뮤니티 게시판 API입니다. 목록·상세(GET)는 로그인 없이 볼 수 있고, 쓰기·고치기·지우기는 로그인 회원만 할 수 있습니다.
 *
 * <ul>
 *   <li>GET /api/community/posts?board=&category=&q=&page=: 목록(20개씩)
 *   <li>GET /api/community/posts/{id}: 상세와 댓글(로그인 상태면 내 글·댓글 표시)
 *   <li>POST /api/community/posts, PUT·DELETE /{id}: 글 쓰기·고치기·지우기
 *   <li>POST /api/community/posts/{id}/comments, DELETE /api/community/comments/{id}: 댓글 달기·지우기
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommunityController {
  private final CommunityService service;

  @GetMapping("/posts")
  public CommunityDtos.PostPage list(
      @RequestParam String board,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page) {
    return service.list(board, category, q, page);
  }

  @GetMapping("/posts/{postId}")
  public CommunityDtos.PostDetail detail(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long postId) {
    return service.detail(jwt == null ? null : JwtMemberId.from(jwt), postId);
  }

  @PostMapping("/posts")
  public CommunityDtos.PostDetail create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CommunityDtos.PostRequest request) {
    return service.create(JwtMemberId.from(jwt), request);
  }

  @PutMapping("/posts/{postId}")
  public CommunityDtos.PostDetail update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId,
      @Valid @RequestBody CommunityDtos.PostRequest request) {
    return service.update(JwtMemberId.from(jwt), postId, request);
  }

  @DeleteMapping("/posts/{postId}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long postId) {
    service.delete(JwtMemberId.from(jwt), postId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/posts/{postId}/comments")
  public CommunityDtos.Comment addComment(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId,
      @Valid @RequestBody CommunityDtos.CommentRequest request) {
    return service.addComment(JwtMemberId.from(jwt), postId, request.body());
  }

  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long commentId) {
    service.deleteComment(JwtMemberId.from(jwt), commentId);
    return ResponseEntity.noContent().build();
  }
}
