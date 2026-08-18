package com.movrit.movrit.review.dto;

import com.movrit.movrit.review.entity.ProductReview;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ProductReviewDtos {
    private ProductReviewDtos() {}

    public record CreateRequest(
            @NotNull Long orderId,
            @NotNull @Min(1) @Max(5) Integer rating,
            @NotBlank @Size(max = 2000) String content
    ) {}

    public record UpdateRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @NotBlank @Size(max = 2000) String content
    ) {}

    public record ReplyRequest(
            @NotBlank @Size(max = 2000) String reply
    ) {}

    public record Response(
            Long id,
            Long orderId,
            Long memberId,
            String memberName,
            Long productId,
            String productName,
            Integer rating,
            String content,
            String sellerReply,
            LocalDateTime sellerRepliedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Response from(ProductReview review) {
            return new Response(
                    review.getId(),
                    review.getOrder().getId(),
                    review.getMember().getId(),
                    review.getMember().getName(),
                    review.getProduct().getId(),
                    review.getProduct().getName(),
                    review.getRating(),
                    review.getContent(),
                    review.getSellerReply(),
                    review.getSellerRepliedAt(),
                    review.getCreatedAt(),
                    review.getUpdatedAt()
            );
        }
    }
}
