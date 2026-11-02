package com.movrit.movrit.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.community.dto.CommunityDtos;
import com.movrit.movrit.community.entity.CommunityComment;
import com.movrit.movrit.community.entity.CommunityPost;
import com.movrit.movrit.community.repository.CommunityCommentRepository;
import com.movrit.movrit.community.repository.CommunityPostRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 운영자 커뮤니티 관리(가리기·다시 보이기, 권한, 상태 값)를 확인합니다. */
class CommunityAdminServiceTest {
  private final CommunityPostRepository posts = mock(CommunityPostRepository.class);
  private final CommunityCommentRepository comments = mock(CommunityCommentRepository.class);
  private final MemberService members = mock(MemberService.class);
  private final NotificationService notifications = mock(NotificationService.class);
  private final AuditLogService audit = mock(AuditLogService.class);
  private final CommunityAdminService service =
      new CommunityAdminService(posts, comments, members, notifications, audit);

  private final Member writer =
      Member.builder().id(1L).nickname("글쓴이").role(MemberRole.USER).build();
  private final Member admin = Member.builder().id(9L).role(MemberRole.ADMIN).build();

  private CommunityPost post() {
    return CommunityPost.builder()
        .id(5L)
        .board("free")
        .category("일상")
        .title("광고 글")
        .body("오픈채팅으로 오세요")
        .author(writer)
        .build();
  }

  @Test
  void onlyAdminCanManage() {
    when(members.getMember(1L)).thenReturn(writer);
    assertThatThrownBy(() -> service.list(1L, null, null, null, 0))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  }

  @Test
  void hideAndUnhidePostNotifiesAuthorAndRejectsSameState() {
    when(members.getMember(9L)).thenReturn(admin);
    CommunityPost post = post();
    when(posts.findById(5L)).thenReturn(Optional.of(post));
    when(comments.findAllByPostIdOrderByIdAsc(5L)).thenReturn(List.of());

    var hidden = service.setPostHidden(9L, 5L, new CommunityDtos.VisibilityRequest(true, "광고"));
    assertThat(hidden.post().status()).isEqualTo("HIDDEN");
    assertThat(post.visible()).isFalse();
    assertThat(post.getHiddenAt()).isNotNull();
    verify(notifications)
        .send(eq(1L), eq("COMMUNITY"), anyString(), anyString(), eq("COMMUNITY"), eq(5L));
    verify(audit)
        .record(eq(9L), eq("COMMUNITY_HIDE"), eq("COMMUNITY_POST"), eq(5L), any(), any(), eq("광고"));

    assertThatThrownBy(
            () -> service.setPostHidden(9L, 5L, new CommunityDtos.VisibilityRequest(true, null)))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

    var shown = service.setPostHidden(9L, 5L, new CommunityDtos.VisibilityRequest(false, null));
    assertThat(shown.post().status()).isEqualTo("VISIBLE");
    assertThat(post.getHiddenAt()).isNull();
  }

  @Test
  void deletedCommentCannotBeChangedButHiddenCommentKeepsOriginalForAdmin() {
    when(members.getMember(9L)).thenReturn(admin);
    CommunityPost post = post();
    CommunityComment comment =
        CommunityComment.builder().id(7L).post(post).author(writer).body("원문 댓글").build();
    when(comments.findById(7L)).thenReturn(Optional.of(comment));
    when(comments.findAllByPostIdOrderByIdAsc(5L)).thenReturn(List.of(comment));

    var result = service.setCommentHidden(9L, 7L, new CommunityDtos.VisibilityRequest(true, null));
    assertThat(result.comments())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.status()).isEqualTo("HIDDEN");
              assertThat(item.body()).isEqualTo("원문 댓글");
            });

    comment.delete();
    assertThatThrownBy(
            () ->
                service.setCommentHidden(9L, 7L, new CommunityDtos.VisibilityRequest(false, null)))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void withdrawnAuthorIsNotNotified() {
    when(members.getMember(9L)).thenReturn(admin);
    CommunityPost post =
        CommunityPost.builder().id(6L).board("free").category("일상").title("t").body("b").build();
    when(posts.findById(6L)).thenReturn(Optional.of(post));
    when(comments.findAllByPostIdOrderByIdAsc(6L)).thenReturn(List.of());
    service.setPostHidden(9L, 6L, new CommunityDtos.VisibilityRequest(true, null));
    verify(notifications, never()).send(any(), any(), any(), any(), any(), any());
  }

  @Test
  void statusAndKeywordAreNormalized() {
    assertThat(CommunityAdminService.normalizeStatus(null)).isEqualTo("ALL");
    assertThat(CommunityAdminService.normalizeStatus("hidden")).isEqualTo("HIDDEN");
    assertThatThrownBy(() -> CommunityAdminService.normalizeStatus("SPAM"))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(CommunityAdminService.escapeLike("100%_달성")).isEqualTo("100\\%\\_달성");
  }
}
