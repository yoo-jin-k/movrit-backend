package com.movrit.movrit.preorder.dto;

import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PreorderDtos {
    private PreorderDtos() {}

    public record CreateRequest(
            @NotNull Long sellerId,
            @NotNull Long productId,
            @NotBlank String title,
            String description,
            @NotNull @DecimalMin("0") BigDecimal preorderPrice,
            @NotBlank String currency,
            @NotNull @PositiveOrZero Long targetQuantity,
            @PositiveOrZero Long maxQuantity,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt,
            LocalDateTime productionExpectedAt,
            LocalDateTime shippingExpectedAt,
            @NotNull PreorderStatus status
    ) {}

    public record UpdateRequest(
            @NotBlank String title,
            String description,
            @NotNull @DecimalMin("0") BigDecimal preorderPrice,
            @NotBlank String currency,
            @NotNull @PositiveOrZero Long targetQuantity,
            @PositiveOrZero Long maxQuantity,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt,
            LocalDateTime productionExpectedAt,
            LocalDateTime shippingExpectedAt,
            @NotNull PreorderStatus status,
            String cancellationReason
    ) {}

    public record Response(
            Long id,
            Long sellerId,
            Long productId,
            String title,
            String description,
            BigDecimal preorderPrice,
            String currency,
            Long targetQuantity,
            Long maxQuantity,
            Long orderedQuantity,
            LocalDateTime startAt,
            LocalDateTime endAt,
            PreorderStatus status
    ) {
        public static Response from(Preorder preorder) {
            return new Response(preorder.getId(), preorder.getSellerId(), preorder.getProduct().getId(),
                    preorder.getTitle(), preorder.getDescription(), preorder.getPreorderPrice(), preorder.getCurrency(),
                    preorder.getTargetQuantity(), preorder.getMaxQuantity(), preorder.getOrderedQuantity(),
                    preorder.getStartAt(), preorder.getEndAt(), preorder.getStatus());
        }
    }
}
