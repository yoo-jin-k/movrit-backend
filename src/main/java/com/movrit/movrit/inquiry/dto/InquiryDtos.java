package com.movrit.movrit.inquiry.dto;

import com.movrit.movrit.inquiry.entity.InquiryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class InquiryDtos {
    private InquiryDtos() {}

    public record CreateRequest(
            Long preorderId,
            @NotBlank @Size(max = 50) String category,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 5000) String content
    ) {}

    public record AnswerRequest(
            @NotBlank @Size(max = 5000) String answer
    ) {}

    public record Response(
            Long id,
            Long memberId,
            String memberName,
            Long preorderId,
            String preorderTitle,
            String category,
            String title,
            String content,
            InquiryStatus status,
            String answer,
            Long answeredByMemberId,
            LocalDateTime answeredAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
