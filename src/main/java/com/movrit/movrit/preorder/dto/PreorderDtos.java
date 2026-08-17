package com.movrit.movrit.preorder.dto;

import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PreorderDtos {
    private PreorderDtos() {}

    public record CreateRequest(
            @NotNull Long productId,
            @NotBlank String title,
            String description,
            @NotNull @DecimalMin("0") BigDecimal preorderPrice,
            @NotBlank String currency,
            @NotNull @Positive Long targetQuantity,
            @Positive Long maxQuantity,
            @NotNull @Min(1) Long minOrderQuantity,
            @Min(1) Long maxOrderQuantityPerMember,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt,
            LocalDateTime productionExpectedAt,
            LocalDateTime shippingExpectedAt
    ) {}

    public record UpdateRequest(
            @NotBlank String title,
            String description,
            @NotNull @DecimalMin("0") BigDecimal preorderPrice,
            @NotBlank String currency,
            @NotNull @Positive Long targetQuantity,
            @Positive Long maxQuantity,
            @NotNull @Min(1) Long minOrderQuantity,
            @Min(1) Long maxOrderQuantityPerMember,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt,
            LocalDateTime productionExpectedAt,
            LocalDateTime shippingExpectedAt
    ) {}

    public record CancelRequest(@NotBlank String reason) {}

    public record Response(
            Long id,
            Long brandId,
            Long productId,
            String productName,
            String title,
            String description,
            BigDecimal preorderPrice,
            String currency,
            Long targetQuantity,
            Long maxQuantity,
            Long orderedQuantity,
            Long minOrderQuantity,
            Long maxOrderQuantityPerMember,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime productionExpectedAt,
            LocalDateTime shippingExpectedAt,
            PreorderStatus status,
            String cancellationReason
    ) {
        public static Response from(Preorder preorder) {
            return new Response(
                    preorder.getId(),
                    preorder.getProduct().getBrand().getId(),
                    preorder.getProduct().getId(),
                    preorder.getProduct().getName(),
                    preorder.getTitle(),
                    preorder.getDescription(),
                    preorder.getPreorderPrice(),
                    preorder.getCurrency(),
                    preorder.getTargetQuantity(),
                    preorder.getMaxQuantity(),
                    preorder.getOrderedQuantity(),
                    preorder.getMinOrderQuantity(),
                    preorder.getMaxOrderQuantityPerMember(),
                    preorder.getStartAt(),
                    preorder.getEndAt(),
                    preorder.getProductionExpectedAt(),
                    preorder.getShippingExpectedAt(),
                    preorder.getStatus(),
                    preorder.getCancellationReason()
            );
        }
    }
}
