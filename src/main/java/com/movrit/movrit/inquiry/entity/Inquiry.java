package com.movrit.movrit.inquiry.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

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

  public void answer(Member admin, String answer) {
    if (status == InquiryStatus.CLOSED) {
      throw new IllegalStateException("종료된 문의에는 답변할 수 없습니다.");
    }
    this.answer = answer;
    answeredBy = admin;
    answeredAt = LocalDateTime.now();
    status = InquiryStatus.ANSWERED;
  }

  public void close() {
    if (status == InquiryStatus.CLOSED) {
      return;
    }
    status = InquiryStatus.CLOSED;
  }
}
