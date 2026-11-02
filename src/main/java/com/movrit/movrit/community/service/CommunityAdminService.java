package com.movrit.movrit.community.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.community.dto.CommunityDtos;
import com.movrit.movrit.community.entity.CommunityComment;
import com.movrit.movrit.community.entity.CommunityPost;
import com.movrit.movrit.community.repository.CommunityCommentRepository;
import com.movrit.movrit.community.repository.CommunityPostRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 운영자의 커뮤니티 관리입니다. 신고가 없어도 글·댓글을 바로 가리거나 다시 보이게 합니다.
 *
 * <ul>
 *   <li>목록: 게시판·상태(보이는 글·가린 글·지운 글)·검색어로 거르고, 글마다 가린 댓글 수를 함께 보여 줍니다.
 *   <li>상세: 가린 댓글과 지운 댓글도 원문 그대로 보여 줍니다(판단 근거).
 *   <li>가리기·다시 보이기: 지운 글·댓글은 바꿀 수 없습니다. 작성자에게 알리고 감사 기록을 남깁니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CommunityAdminService {
  static final int PAGE_SIZE = 20;
  static final Set<String> STATUSES = Set.of("ALL", "VISIBLE", "HIDDEN", "DELETED");
  private static final Set<String> BOARDS = Set.of("free", "neighbor", "guide", "event");

  private final CommunityPostRepository postRepository;
  private final CommunityCommentRepository commentRepository;
  private final MemberService memberService;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  /** 운영자용 글 목록 한 페이지입니다. 게시판이 비어 있으면 모든 게시판을 봅니다. */
  @Transactional(readOnly = true)
  public CommunityDtos.AdminPostPage list(
      Long adminMemberId, String board, String status, String keyword, int page) {
    requireAdmin(adminMemberId);
    String boardFilter = board == null || board.isBlank() ? null : board.trim();
    if (boardFilter != null && !BOARDS.contains(boardFilter)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판을 확인해 주세요.");
    }
    String statusFilter = normalizeStatus(status);
    String keywordFilter =
        keyword == null || keyword.isBlank()
            ? null
            : "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
    Page<CommunityPost> result =
        postRepository.adminSearch(
            boardFilter, keywordFilter, statusFilter, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    return new CommunityDtos.AdminPostPage(
        result.getContent().stream().map(this::summary).toList(),
        result.getNumber(),
        result.getTotalPages(),
        result.getTotalElements());
  }

  /** 운영자용 글 상세입니다. 가린·지운 댓글도 포함합니다. */
  @Transactional(readOnly = true)
  public CommunityDtos.AdminPostDetail detail(Long adminMemberId, Long postId) {
    requireAdmin(adminMemberId);
    return detailView(getPost(postId));
  }

  /**
   * 글을 가리거나 다시 보이게 합니다.
   *
   * @throws ResponseStatusException 운영자가 아니면 403, 글이 없으면 404, 지운 글이거나 이미 그 상태면 409
   */
  @Transactional
  public CommunityDtos.AdminPostDetail setPostHidden(
      Long adminMemberId, Long postId, CommunityDtos.VisibilityRequest request) {
    requireAdmin(adminMemberId);
    CommunityPost post = getPost(postId);
    if (post.getDeletedAt() != null) throw conflict("작성자나 운영자가 지운 글은 바꿀 수 없습니다.");
    if (post.isHidden() == request.hidden()) {
      throw conflict(request.hidden() ? "이미 가린 글입니다." : "이미 보이는 글입니다.");
    }
    if (request.hidden()) post.hideByAdmin();
    else post.unhideByAdmin();
    String note = note(request);
    notifyAuthor(post.getAuthor(), "글", post.getTitle(), request.hidden(), note, post.getId());
    audit(adminMemberId, "COMMUNITY_POST", post.getId(), request.hidden(), note);
    return detailView(post);
  }

  /**
   * 댓글을 가리거나 다시 보이게 합니다.
   *
   * @throws ResponseStatusException 운영자가 아니면 403, 댓글이 없으면 404, 지운 댓글이거나 이미 그 상태면 409
   */
  @Transactional
  public CommunityDtos.AdminPostDetail setCommentHidden(
      Long adminMemberId, Long commentId, CommunityDtos.VisibilityRequest request) {
    requireAdmin(adminMemberId);
    CommunityComment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
    if (comment.getDeletedAt() != null) throw conflict("작성자나 운영자가 지운 댓글은 바꿀 수 없습니다.");
    if (comment.isHidden() == request.hidden()) {
      throw conflict(request.hidden() ? "이미 가린 댓글입니다." : "이미 보이는 댓글입니다.");
    }
    if (request.hidden()) comment.hideByAdmin();
    else comment.unhideByAdmin();
    String note = note(request);
    notifyAuthor(
        comment.getAuthor(),
        "댓글",
        comment.getBody(),
        request.hidden(),
        note,
        comment.getPost().getId());
    audit(adminMemberId, "COMMUNITY_COMMENT", comment.getId(), request.hidden(), note);
    return detailView(comment.getPost());
  }

  // ───────── 내부 규칙 ─────────

  /** 상태 값은 대소문자를 가리지 않고, 비어 있으면 전체입니다. */
  static String normalizeStatus(String status) {
    String value =
        status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
    if (!STATUSES.contains(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상태를 확인해 주세요.");
    }
    return value;
  }

  /** LIKE 검색에서 %·_를 글자 그대로 찾도록 바꿉니다. 쿼리의 escape '\' 와 짝을 이룹니다. */
  static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  static String postStatus(CommunityPost post) {
    if (post.getDeletedAt() != null) return "DELETED";
    return post.isHidden() ? "HIDDEN" : "VISIBLE";
  }

  static String commentStatus(CommunityComment comment) {
    if (comment.getDeletedAt() != null) return "DELETED";
    return comment.isHidden() ? "HIDDEN" : "VISIBLE";
  }

  private CommunityPost getPost(Long postId) {
    return postRepository
        .findById(postId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
  }

  private CommunityDtos.AdminPostSummary summary(CommunityPost post) {
    String body = post.getBody();
    Member author = post.getAuthor();
    return new CommunityDtos.AdminPostSummary(
        post.getId(),
        post.getBoard(),
        post.getCategory(),
        post.getTitle(),
        body.length() > 80 ? body.substring(0, 80) + "…" : body,
        author == null ? null : author.getId(),
        CommunityService.authorName(author),
        post.getViewCount(),
        post.getCommentCount(),
        commentRepository.countByPostIdAndHiddenTrueAndDeletedAtIsNull(post.getId()),
        postStatus(post),
        post.getHiddenAt(),
        post.getCreatedAt());
  }

  private CommunityDtos.AdminPostDetail detailView(CommunityPost post) {
    List<CommunityDtos.AdminComment> comments =
        commentRepository.findAllByPostIdOrderByIdAsc(post.getId()).stream()
            .map(
                comment ->
                    new CommunityDtos.AdminComment(
                        comment.getId(),
                        comment.getAuthor() == null ? null : comment.getAuthor().getId(),
                        CommunityService.authorName(comment.getAuthor()),
                        comment.getBody(),
                        commentStatus(comment),
                        comment.getHiddenAt(),
                        comment.getCreatedAt()))
            .toList();
    return new CommunityDtos.AdminPostDetail(summary(post), post.getBody(), comments);
  }

  /** 작성자에게 결과를 알립니다. 탈퇴했거나 작성자를 알 수 없으면 보내지 않습니다. */
  private void notifyAuthor(
      Member author, String label, String text, boolean hidden, String note, Long postId) {
    if (author == null || author.getDeletedAt() != null) return;
    String preview = text.length() > 30 ? text.substring(0, 30) + "…" : text;
    notificationService.send(
        author.getId(),
        "COMMUNITY",
        hidden ? "운영 정책에 따라 커뮤니티 " + label + "이 가려졌습니다." : "가려졌던 커뮤니티 " + label + "이 다시 보입니다.",
        preview + (note == null ? "" : " · " + note),
        "COMMUNITY",
        postId);
  }

  private void audit(Long adminMemberId, String type, Long id, boolean hidden, String note) {
    auditLogService.record(
        adminMemberId,
        hidden ? "COMMUNITY_HIDE" : "COMMUNITY_UNHIDE",
        type,
        id,
        "{\"hidden\":" + !hidden + "}",
        "{\"hidden\":" + hidden + ",\"at\":\"" + LocalDateTime.now() + "\"}",
        note);
  }

  private static String note(CommunityDtos.VisibilityRequest request) {
    return request.note() == null || request.note().isBlank() ? null : request.note().strip();
  }

  private void requireAdmin(Long memberId) {
    if (!memberService.getMember(memberId).isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영자만 할 수 있습니다.");
    }
  }

  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
