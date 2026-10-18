package com.movrit.movrit.inquiry.dto;

import com.movrit.movrit.inquiry.entity.Inquiry;
import com.movrit.movrit.inquiry.entity.InquiryStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public final class InquiryDtos {
  private InquiryDtos() {}

  public record CreateRequest(
      Long preorderId,
      @NotBlank String category,
      @NotBlank String title,
      @NotBlank String content) {}

  public record AnswerRequest(@NotBlank String answer) {}

  public record Response(
      Long id,
      Long memberId,
      Long preorderId,
      String category,
      String title,
      String content,
      InquiryStatus status,
      String answer,
      Long answeredByMemberId,
      LocalDateTime answeredAt,
      LocalDateTime createdAt) {
    public static Response from(Inquiry inquiry) {
      return new Response(
          inquiry.getId(),
          inquiry.getMember().getId(),
          inquiry.getPreorder() == null ? null : inquiry.getPreorder().getId(),
          inquiry.getCategory(),
          inquiry.getTitle(),
          inquiry.getContent(),
          inquiry.getStatus(),
          inquiry.getAnswer(),
          inquiry.getAnsweredBy() == null ? null : inquiry.getAnsweredBy().getId(),
          inquiry.getAnsweredAt(),
          inquiry.getCreatedAt());
    }
  }
}
