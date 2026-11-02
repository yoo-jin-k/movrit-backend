package com.movrit.movrit.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.movrit.movrit.community.dto.CommunityDtos;
import com.movrit.movrit.community.entity.CommunityPost;
import com.movrit.movrit.community.repository.CommunityCommentRepository;
import com.movrit.movrit.community.repository.CommunityPostRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.service.MemberService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 커뮤니티 게시판 권한(운영자 전용 게시판·본인 글 수정·삭제)을 확인합니다. */
class CommunityServiceTest {
  private final CommunityPostRepository posts = mock(CommunityPostRepository.class);
  private final CommunityCommentRepository comments = mock(CommunityCommentRepository.class);
  private final MemberService members = mock(MemberService.class);
  private final CommunityService service = new CommunityService(posts, comments, members);

  private final Member user = Member.builder().id(1L).nickname("하나").role(MemberRole.USER).build();
  private final Member other = Member.builder().id(2L).nickname("둘").role(MemberRole.USER).build();
  private final Member admin = Member.builder().id(9L).role(MemberRole.ADMIN).build();

  @Test
  void userCannotWriteToOperatorBoard() {
    when(members.getMember(1L)).thenReturn(user);
    assertThatThrownBy(
            () ->
                service.create(
                    1L, new CommunityDtos.PostRequest("event", "이벤트", "제목입니다", "본문입니다 다섯자 이상")))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  }

  @Test
  void userWritesFreeBoardAndOnlyAuthorOrAdminCanDelete() {
    when(members.getMember(1L)).thenReturn(user);
    when(members.getMember(2L)).thenReturn(other);
    when(members.getMember(9L)).thenReturn(admin);
    when(posts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var created =
        service.create(1L, new CommunityDtos.PostRequest("free", "일상", " 안녕하세요 ", "처음 글을 남깁니다."));
    assertThat(created.title()).isEqualTo("안녕하세요");
    assertThat(created.mine()).isTrue();

    CommunityPost post =
        CommunityPost.builder()
            .id(5L)
            .board("free")
            .category("일상")
            .title("t")
            .body("b")
            .author(user)
            .build();
    when(posts.findById(5L)).thenReturn(Optional.of(post));
    when(comments.findAllByPostIdAndDeletedAtIsNullOrderByIdAsc(5L)).thenReturn(List.of());
    assertThatThrownBy(() -> service.delete(2L, 5L)).isInstanceOf(ResponseStatusException.class);
    service.delete(9L, 5L);
    assertThat(post.visible()).isFalse();
  }

  @Test
  void authorNameHidesWithdrawnAndMarksOperator() {
    assertThat(CommunityService.authorName(null)).isEqualTo("탈퇴한 회원");
    assertThat(CommunityService.authorName(admin)).isEqualTo("MOVRIT 운영팀");
    assertThat(CommunityService.authorName(user)).isEqualTo("하나");
  }
}
