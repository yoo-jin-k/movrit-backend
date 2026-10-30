package com.movrit.movrit.inquiry.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** 회원이 남기는 1:1 문의입니다. 운영자가 답변하면 답변 내용·답변자·시각이 기록되고 상태가 바뀝니다. */
@Entity
@Table(name = "inquiry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Inquiry extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "preorder_id")
  private Preorder preorder;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InquiryStatus status;

  @Column(columnDefinition = "TEXT")
  private String answer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answered_by_member_id")
  private Member answeredBy;

  @Column(name = "answered_at")
  private LocalDateTime answeredAt;

  /** 운영자 답변을 등록하고 상태를 ANSWERED로 바꿉니다. 종료(CLOSED)된 문의에는 답변할 수 없습니다. */
  public void answer(Member admin, String answer) {
    if (status == InquiryStatus.CLOSED) {
      throw new IllegalStateException("종료된 문의에는 답변할 수 없습니다.");
    }
    this.answer = answer;
    answeredBy = admin;
    answeredAt = LocalDateTime.now();
    status = InquiryStatus.ANSWERED;
  }

  /** 문의를 종료합니다. 이미 종료된 문의는 그대로 둡니다. */
  public void close() {
    if (status == InquiryStatus.CLOSED) {
      return;
    }
    status = InquiryStatus.CLOSED;
  }
}
