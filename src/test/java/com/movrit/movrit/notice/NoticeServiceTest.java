package com.movrit.movrit.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notice.dto.NoticeDtos;
import com.movrit.movrit.notice.entity.Notice;
import com.movrit.movrit.notice.entity.NoticeCategory;
import com.movrit.movrit.notice.repository.NoticeRepository;
import com.movrit.movrit.notice.service.NoticeService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** 공지 작성·게시 규칙을 확인합니다. */
class NoticeServiceTest {
  private final NoticeRepository notices = mock(NoticeRepository.class);
  private final MemberService members = mock(MemberService.class);
  private final NoticeService service = new NoticeService(notices, members);

  @Test
  void publishTimeIsRecordedOnlyOnFirstPublish() {
    when(members.getAdmin(1L)).thenReturn(Member.builder().id(1L).name("운영팀").build());
    when(notices.save(any(Notice.class))).thenAnswer(invocation -> invocation.getArgument(0));

    NoticeDtos.Response draft =
        service.create(
            1L, new NoticeDtos.SaveRequest(" 제목 ", " 본문 ", NoticeCategory.NOTICE, false, false));
    // 게시 전에는 게시 시각이 없고, 앞뒤 공백은 지웁니다.
    assertThat(draft.publishedAt()).isNull();
    assertThat(draft.title()).isEqualTo("제목");

    Notice notice = Notice.builder().title("t").body("b").category(NoticeCategory.NOTICE).build();
    notice.edit("t", "b", NoticeCategory.NOTICE, false, true);
    var first = notice.getPublishedAt();
    notice.edit("t", "b", NoticeCategory.NOTICE, false, false);
    notice.edit("t2", "b", NoticeCategory.NOTICE, true, true);
    // 내렸다가 다시 게시해도 처음 게시 시각을 유지합니다.
    assertThat(notice.getPublishedAt()).isEqualTo(first);
  }

  @Test
  void unpublishedNoticeIsHiddenFromUsers() {
    when(notices.findByIdAndPublishedTrue(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.publishedOne(5L)).isInstanceOf(ResponseStatusException.class);
  }
}
