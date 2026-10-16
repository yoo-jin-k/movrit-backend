package com.movrit.movrit.inquiry.entity;

import com.movrit.movrit.member.entity.Member;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryTest {

    @Test
    void openInquiryCanBeAnsweredAndClosed() {
        Inquiry inquiry = Inquiry.builder().status(InquiryStatus.OPEN).build();
        Member admin = Member.builder().id(7L).build();

        inquiry.answer(admin, "확인 후 안내드립니다.");
        inquiry.close();

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.CLOSED);
        assertThat(inquiry.getAnsweredBy()).isSameAs(admin);
        assertThat(inquiry.getAnsweredAt()).isNotNull();
    }

    @Test
    void closedInquiryCannotBeAnswered() {
        Inquiry inquiry = Inquiry.builder().status(InquiryStatus.CLOSED).build();
        Member admin = Member.builder().id(7L).build();

        assertThatThrownBy(() -> inquiry.answer(admin, "답변"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("종료된 문의에는 답변할 수 없습니다.");
    }
}
