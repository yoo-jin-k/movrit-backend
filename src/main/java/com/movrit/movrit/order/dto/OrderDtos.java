package com.movrit.movrit.order.dto;

import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderItem;
import com.movrit.movrit.order.entity.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record CreateRequest(
            @NotBlank String requestKey,
            @NotNull Long preorderId,
            @NotNull Long productVariantId,
            @NotNull @Min(1) Long quantity,
            String referralCode,
            String acquisitionSource,
            @NotBlank String receiverName,
            @NotBlank String receiverPhone,
            String postalCode,
            @NotBlank String address1,
            String address2
    ) {}

    public record ReceiverUpdateRequest(
            @NotBlank String receiverName,
            @NotBlank String receiverPhone,
            String postalCode,
            @NotBlank String address1,
            String address2
    ) {}

    public record ItemResponse(Long id, Long productId, Long productVariantId, String productName,
                               String optionName, BigDecimal unitPrice, Long quantity, BigDecimal totalPrice) {
        public static ItemResponse from(OrderItem item) {
            return new ItemResponse(item.getId(), item.getProduct().getId(), item.getProductVariantId(),
                    item.getProductName(), item.getOptionName(), item.getUnitPrice(), item.getQuantity(), item.getTotalPrice());
        }
    }

    public record Response(
            Long id, String orderNumber, Long memberId, Long preorderId, String preorderTitle,
            OrderStatus status, BigDecimal totalProductAmount, BigDecimal discountAmount,
            BigDecimal shippingAmount, BigDecimal totalPaymentAmount, String currency,
            String receiverName, String receiverPhone, String postalCode, String address1, String address2,
            LocalDateTime paidAt, LocalDateTime cancelledAt, List<ItemResponse> items
    ) {
        public static Response from(Order order, List<OrderItem> items) {
            return new Response(order.getId(), order.getOrderNumber(), order.getMember().getId(),
                    order.getPreorder().getId(), order.getPreorder().getTitle(), order.getStatus(),
                    order.getTotalProductAmount(), order.getDiscountAmount(), order.getShippingAmount(),
                    order.getTotalPaymentAmount(), order.getCurrency(), order.getReceiverName(),
                    order.getReceiverPhone(), order.getPostalCode(), order.getAddress1(), order.getAddress2(),
                    order.getPaidAt(), order.getCancelledAt(), items.stream().map(ItemResponse::from).toList());
        }
    }
}
