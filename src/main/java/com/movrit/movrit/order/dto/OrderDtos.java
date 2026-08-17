package com.movrit.movrit.order.dto;

import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record ItemRequest(
            @NotNull Long preorderVariantId,
            @NotNull @Min(1) Long quantity
    ) {}

    public record CreateRequest(
            @NotBlank String orderNumber,
            @NotBlank String requestKey,
            @NotNull Long memberId,
            @NotNull Long preorderId,
            Long attributionId,
            @NotBlank String acquisitionSource,
            OrderStatus status,
            BigDecimal totalProductAmount,
            @NotNull @DecimalMin("0") BigDecimal discountAmount,
            @NotNull @DecimalMin("0") BigDecimal shippingAmount,
            BigDecimal totalPaymentAmount,
            @NotBlank String currency,
            @NotBlank String receiverName,
            @NotBlank String receiverPhone,
            String postalCode,
            @NotBlank String address1,
            String address2,
            @NotEmpty List<@Valid ItemRequest> items
    ) {}

    public record UpdateRequest(
            @NotNull OrderStatus status,
            @NotBlank String receiverName,
            @NotBlank String receiverPhone,
            String postalCode,
            @NotBlank String address1,
            String address2,
            LocalDateTime paidAt,
            LocalDateTime cancelledAt
    ) {}

    public record Response(
            Long id,
            String orderNumber,
            Long memberId,
            Long preorderId,
            OrderStatus status,
            BigDecimal totalPaymentAmount,
            String currency,
            String receiverName,
            String receiverPhone,
            String address1,
            String address2
    ) {
        public static Response from(Order order) {
            return new Response(order.getId(), order.getOrderNumber(), order.getMember().getId(),
                    order.getPreorder().getId(), order.getStatus(), order.getTotalPaymentAmount(), order.getCurrency(),
                    order.getReceiverName(), order.getReceiverPhone(), order.getAddress1(), order.getAddress2());
        }
    }
}
