package com.movrit.movrit.favorite.dto;

import com.movrit.movrit.preorder.entity.PreorderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FavoriteDtos {
    public record Response(
            Long favoriteId,
            Long preorderId,
            String title,
            BigDecimal preorderPrice,
            String currency,
            Long targetQuantity,
            Long orderedQuantity,
            LocalDateTime startAt,
            LocalDateTime endAt,
            PreorderStatus status
    ) {}

    public record StatusResponse(boolean favorite) {}
}
