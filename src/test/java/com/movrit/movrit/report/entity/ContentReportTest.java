package com.movrit.movrit.report.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 신고 처리 상태 규칙을 확인합니다. */
class ContentReportTest {

  @Test
  void dismissAndResolveAreHandledOnce() {
    ContentReport dismissed =
        new ContentReport(1L, "COMMUNITY_POST", 3L, 2L, "[제목] 본문", "SPAM", null);
    dismissed.handle("DISMISS", null, 9L);
    assertThat(dismissed.getStatus()).isEqualTo(ContentReport.DISMISSED);
    assertThatThrownBy(() -> dismissed.handle("HIDE", null, 9L))
        .isInstanceOf(IllegalStateException.class);

    ContentReport hidden = new ContentReport(1L, "CHAT_MESSAGE", 4L, 2L, "욕설", "ABUSE", "반복");
    hidden.handle("HIDE", "욕설 반복", 9L);
    assertThat(hidden.getStatus()).isEqualTo(ContentReport.RESOLVED);
    assertThat(hidden.getResolution()).isEqualTo("HIDE");
    assertThat(hidden.getHandledAt()).isNotNull();
  }
}
