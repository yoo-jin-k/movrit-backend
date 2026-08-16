package com.movrit.movrit.shipping.dto;

import com.movrit.movrit.shipping.entity.Shipping;
import com.movrit.movrit.shipping.entity.ShippingStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class ShippingDtos {
    private ShippingDtos() {}

    public record CreateRequest(
            @NotNull Long orderId,
            String courierCode,
            String trackingNumber,
            @NotNull ShippingStatus status,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt
    ) {}

    public record UpdateRequest(
            String courierCode,
            String trackingNumber,
            @NotNull ShippingStatus status,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt
    ) {}

    public record Response(
            Long id,
            Long orderId,
            String courierCode,
            String trackingNumber,
            ShippingStatus status,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt
    ) {
        public static Response from(Shipping shipping) {
            return new Response(shipping.getId(), shipping.getOrder().getId(), shipping.getCourierCode(),
                    shipping.getTrackingNumber(), shipping.getStatus(), shipping.getShippedAt(), shipping.getDeliveredAt());
        }
    }
}
