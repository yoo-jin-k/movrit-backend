package com.movrit.movrit.community.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.community.dto.CommunityDtos;
import com.movrit.movrit.community.service.CommunityAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 커뮤니티 관리 API입니다(운영자만).
 *
 * <ul>
 *   <li>GET /api/admin/community/posts?board=&status=ALL|VISIBLE|HIDDEN|DELETED&q=&page=
 *   <li>GET /api/admin/community/posts/{postId}: 가린·지운 댓글을 포함한 상세
 *   <li>PATCH /api/admin/community/posts/{postId}/visibility: 글 가리기·다시 보이기
 *   <li>PATCH /api/admin/community/comments/{commentId}/visibility: 댓글 가리기·다시 보이기
 * </ul>
 */
@Tag(name = "운영자 · 커뮤니티 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/community")
public class CommunityAdminController {
  private final CommunityAdminService service;

  /** 운영자가 숨김 글을 포함한 커뮤니티 글 목록을 조회합니다. */
  @GetMapping("/posts")
  public CommunityDtos.AdminPostPage list(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) String board,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page) {
    return service.list(JwtMemberId.from(jwt), board, status, q, page);
  }

  /** 운영자가 숨김 댓글을 포함한 글 상세를 조회합니다. */
  @GetMapping("/posts/{postId}")
  public CommunityDtos.AdminPostDetail detail(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long postId) {
    return service.detail(JwtMemberId.from(jwt), postId);
  }

  /** 운영자가 글을 숨기거나 다시 보이게 합니다. */
  @PatchMapping("/posts/{postId}/visibility")
  public CommunityDtos.AdminPostDetail setPostHidden(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId,
      @Valid @RequestBody CommunityDtos.VisibilityRequest request) {
    return service.setPostHidden(JwtMemberId.from(jwt), postId, request);
  }

  /** 운영자가 댓글을 숨기거나 다시 보이게 합니다. */
  @PatchMapping("/comments/{commentId}/visibility")
  public CommunityDtos.AdminPostDetail setCommentHidden(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long commentId,
      @Valid @RequestBody CommunityDtos.VisibilityRequest request) {
    return service.setCommentHidden(JwtMemberId.from(jwt), commentId, request);
  }
}
