package com.movrit.movrit.inquiry.dto;

import com.movrit.movrit.inquiry.entity.Inquiry;
import com.movrit.movrit.inquiry.entity.InquiryStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/** 문의 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
public final class InquiryDtos {
  private InquiryDtos() {}

  /**
   * 문의의 생성 요청을 전달합니다.
   *
   * @param preorderId 연결된 프리오더 식별자입니다.
   * @param category 분류값입니다. 빈 문자열을 허용하지 않습니다.
   * @param title 제목입니다. 빈 문자열을 허용하지 않습니다.
   * @param content 문의 본문입니다. 빈 문자열을 허용하지 않습니다.
   */
  public record CreateRequest(
      Long preorderId,
      @NotBlank String category,
      @NotBlank String title,
      @NotBlank String content) {}

  /**
   * 문의의 문의 답변 요청을 전달합니다.
   *
   * @param answer 문의 답변입니다. 빈 문자열을 허용하지 않습니다.
   */
  public record AnswerRequest(@NotBlank String answer) {}

  /**
   * 문의의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param memberId 회원 식별자입니다.
   * @param preorderId 연결된 프리오더 식별자입니다.
   * @param category 분류값입니다.
   * @param title 제목입니다.
   * @param content 문의 본문입니다.
   * @param status 현재 업무 상태입니다.
   * @param answer 문의 답변입니다.
   * @param answeredByMemberId 답변을 작성한 회원 식별자입니다.
   * @param answeredAt 답변 시각입니다.
   * @param createdAt 생성 시각입니다.
   */
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
    /** 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
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
