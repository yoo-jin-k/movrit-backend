package com.movrit.movrit.community.service;

import com.movrit.movrit.community.dto.CommunityDtos;
import com.movrit.movrit.community.entity.CommunityComment;
import com.movrit.movrit.community.entity.CommunityPost;
import com.movrit.movrit.community.repository.CommunityCommentRepository;
import com.movrit.movrit.community.repository.CommunityPostRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 커뮤니티 게시판(글·댓글)을 처리합니다.
 *
 * <ul>
 *   <li>목록·상세는 로그인하지 않아도 볼 수 있습니다. 상세를 열면 조회수가 1 오릅니다.
 *   <li>free·neighbor 게시판은 회원 누구나, guide·event 게시판은 운영자만 글을 씁니다.
 *   <li>글·댓글 수정·삭제는 쓴 사람만, 삭제는 운영자도 할 수 있습니다. 삭제는 기록만 남기고 화면에서 숨깁니다.
 *   <li>정지·탈퇴 회원은 글과 댓글을 쓸 수 없습니다(회원 조회 단계에서 막힙니다).
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CommunityService {
  static final int PAGE_SIZE = 20;
  private static final Set<String> BOARDS = Set.of("free", "neighbor", "guide", "event");
  private static final Set<String> MEMBER_BOARDS = Set.of("free", "neighbor");
  private static final Set<String> CATEGORIES = Set.of("일상", "체험단", "정보공유", "질문하기", "공지", "이벤트");

  private final CommunityPostRepository postRepository;
  private final CommunityCommentRepository commentRepository;
  private final MemberService memberService;

  /** 게시판 목록 한 페이지입니다. 분류 '전체'와 빈 검색어는 거르지 않습니다. */
  @Transactional(readOnly = true)
  public CommunityDtos.PostPage list(String board, String category, String keyword, int page) {
    requireBoard(board);
    String categoryFilter =
        category == null || category.isBlank() || "전체".equals(category) ? null : category;
    String keywordFilter =
        keyword == null || keyword.isBlank()
            ? null
            : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    Page<CommunityPost> result =
        postRepository.search(
            board, categoryFilter, keywordFilter, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    return new CommunityDtos.PostPage(
        result.getContent().stream().map(CommunityService::summary).toList(),
        result.getNumber(),
        result.getTotalPages(),
        result.getTotalElements());
  }

  /** 글 상세와 댓글입니다. 조회수를 1 올립니다. 지웠거나 숨긴 글이면 404입니다. */
  @Transactional
  public CommunityDtos.PostDetail detail(Long memberId, Long postId) {
    CommunityPost post = visiblePost(postId);
    post.increaseView();
    return detailView(post, viewer(memberId));
  }

  /** 글을 씁니다. guide·event 게시판은 운영자만 쓸 수 있습니다(403). */
  @Transactional
  public CommunityDtos.PostDetail create(Long memberId, CommunityDtos.PostRequest request) {
    Member me = memberService.getMember(memberId);
    requireBoard(request.board());
    if (!MEMBER_BOARDS.contains(request.board()) && !me.isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 게시판은 운영자만 글을 쓸 수 있습니다.");
    }
    requireCategory(request.category());
    CommunityPost post =
        postRepository.save(
            CommunityPost.builder()
                .board(request.board())
                .category(request.category())
                .title(request.title().trim())
                .body(request.body().trim())
                .author(me)
                .build());
    return detailView(post, me);
  }

  /** 내가 쓴 글을 고칩니다. 게시판은 바꿀 수 없습니다. */
  @Transactional
  public CommunityDtos.PostDetail update(
      Long memberId, Long postId, CommunityDtos.PostRequest request) {
    Member me = memberService.getMember(memberId);
    CommunityPost post = visiblePost(postId);
    if (!isAuthor(post.getAuthor(), me)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 쓴 글만 고칠 수 있습니다.");
    }
    requireCategory(request.category());
    post.edit(request.category(), request.title().trim(), request.body().trim());
    return detailView(post, me);
  }

  /** 글을 지웁니다(쓴 사람·운영자). */
  @Transactional
  public void delete(Long memberId, Long postId) {
    Member me = memberService.getMember(memberId);
    CommunityPost post = visiblePost(postId);
    if (!isAuthor(post.getAuthor(), me) && !me.isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 쓴 글만 지울 수 있습니다.");
    }
    post.delete();
  }

  /** 댓글을 답니다. */
  @Transactional
  public CommunityDtos.Comment addComment(Long memberId, Long postId, String body) {
    Member me = memberService.getMember(memberId);
    CommunityPost post = visiblePost(postId);
    String text = body == null ? "" : body.strip();
    if (text.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "댓글을 입력해 주세요.");
    CommunityComment comment =
        commentRepository.save(CommunityComment.builder().post(post).author(me).body(text).build());
    post.commentAdded();
    return comment(comment, me);
  }

  /** 댓글을 지웁니다(쓴 사람·운영자). */
  @Transactional
  public void deleteComment(Long memberId, Long commentId) {
    Member me = memberService.getMember(memberId);
    CommunityComment comment =
        commentRepository
            .findById(commentId)
            .filter(value -> value.getDeletedAt() == null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
    if (!isAuthor(comment.getAuthor(), me) && !me.isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 쓴 댓글만 지울 수 있습니다.");
    }
    comment.delete();
    comment.getPost().commentRemoved();
  }

  // ───────── 내부 규칙 ─────────

  private Member viewer(Long memberId) {
    if (memberId == null) return null;
    try {
      return memberService.getMember(memberId);
    } catch (ResponseStatusException exception) {
      return null;
    }
  }

  private CommunityPost visiblePost(Long postId) {
    return postRepository
        .findById(postId)
        .filter(CommunityPost::visible)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
  }

  private static void requireBoard(String board) {
    if (!BOARDS.contains(board)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다.");
    }
  }

  private static void requireCategory(String category) {
    if (!CATEGORIES.contains(category)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "분류를 다시 선택해 주세요.");
    }
  }

  private static boolean isAuthor(Member author, Member me) {
    return author != null && me != null && Objects.equals(author.getId(), me.getId());
  }

  private CommunityDtos.PostDetail detailView(CommunityPost post, Member viewer) {
    List<CommunityDtos.Comment> comments =
        post.getId() == null
            ? List.of()
            : commentRepository.findAllByPostIdAndDeletedAtIsNullOrderByIdAsc(post.getId()).stream()
                .map(comment -> comment(comment, viewer))
                .toList();
    boolean mine = isAuthor(post.getAuthor(), viewer);
    return new CommunityDtos.PostDetail(
        post.getId(),
        post.getBoard(),
        post.getCategory(),
        post.getTitle(),
        post.getBody(),
        authorName(post.getAuthor()),
        post.getViewCount(),
        post.getCreatedAt(),
        post.getUpdatedAt(),
        mine,
        mine || (viewer != null && viewer.isAdmin()),
        comments);
  }

  /** 운영자가 가린 댓글 자리에 보여 줄 문구입니다. */
  static final String HIDDEN_COMMENT_TEXT = "운영 정책에 따라 운영자가 가린 댓글입니다.";

  private static CommunityDtos.Comment comment(CommunityComment comment, Member viewer) {
    boolean mine = isAuthor(comment.getAuthor(), viewer);
    boolean hidden = comment.isHidden();
    return new CommunityDtos.Comment(
        comment.getId(),
        authorName(comment.getAuthor()),
        hidden ? HIDDEN_COMMENT_TEXT : comment.getBody(),
        comment.getCreatedAt(),
        mine,
        mine || (viewer != null && viewer.isAdmin()),
        hidden);
  }

  private static CommunityDtos.PostSummary summary(CommunityPost post) {
    String body = post.getBody();
    return new CommunityDtos.PostSummary(
        post.getId(),
        post.getBoard(),
        post.getCategory(),
        post.getTitle(),
        body.length() > 80 ? body.substring(0, 80) + "…" : body,
        authorName(post.getAuthor()),
        post.getViewCount(),
        post.getCommentCount(),
        post.getCreatedAt());
  }

  /** 작성자 표시 이름입니다. 운영자는 'MOVRIT 운영팀', 탈퇴 회원은 '탈퇴한 회원'입니다. */
  static String authorName(Member author) {
    if (author == null || author.getDeletedAt() != null) return "탈퇴한 회원";
    if (author.isAdmin()) return "MOVRIT 운영팀";
    if (author.getNickname() != null && !author.getNickname().isBlank())
      return author.getNickname();
    return "회원";
  }
}
