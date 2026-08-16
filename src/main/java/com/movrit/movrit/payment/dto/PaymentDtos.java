package com.movrit.movrit.payment.dto;

import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record CreateRequest(
            @NotNull Long orderId,
            String paymentKey,
            String pgProvider,
            String method,
            @NotNull PaymentStatus status,
            @NotNull @DecimalMin("0") BigDecimal requestedAmount,
            BigDecimal approvedAmount,
            @NotBlank String currency,
            String idempotencyKey,
            LocalDateTime requestedAt,
            LocalDateTime approvedAt,
            LocalDateTime failedAt,
            String failureCode,
            String failureMessage
    ) {}

    public record UpdateRequest(
            String paymentKey,
            String pgProvider,
            String method,
            @NotNull PaymentStatus status,
            BigDecimal approvedAmount,
            LocalDateTime requestedAt,
            LocalDateTime approvedAt,
            LocalDateTime failedAt,
            String failureCode,
            String failureMessage
    ) {}

    public record Response(
            Long id,
            Long orderId,
            String paymentKey,
            String pgProvider,
            String method,
            PaymentStatus status,
            BigDecimal requestedAmount,
            BigDecimal approvedAmount,
            String currency
    ) {
        public static Response from(Payment payment) {
            return new Response(payment.getId(), payment.getOrder().getId(), payment.getPaymentKey(),
                    payment.getPgProvider(), payment.getMethod(), payment.getStatus(), payment.getRequestedAmount(),
                    payment.getApprovedAmount(), payment.getCurrency());
        }
    }
}
