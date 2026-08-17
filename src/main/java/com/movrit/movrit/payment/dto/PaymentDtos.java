package com.movrit.movrit.payment.dto;

import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentDtos {
    private PaymentDtos() {}
    public record CreateRequest(@NotNull Long orderId, @NotBlank String idempotencyKey) {}
    public record ApproveRequest(@NotBlank String paymentKey, @NotBlank String pgProvider, @NotBlank String method) {}
    public record FailRequest(@NotBlank String failureCode, @NotBlank String failureMessage) {}
    public record Response(Long id, Long orderId, String paymentKey, String pgProvider, String method,
                           PaymentStatus status, BigDecimal requestedAmount, BigDecimal approvedAmount,
                           String currency, LocalDateTime requestedAt, LocalDateTime approvedAt,
                           LocalDateTime failedAt, String failureCode, String failureMessage) {
        public static Response from(Payment p) {
            return new Response(p.getId(), p.getOrder().getId(), p.getPaymentKey(), p.getPgProvider(), p.getMethod(),
                    p.getStatus(), p.getRequestedAmount(), p.getApprovedAmount(), p.getCurrency(), p.getRequestedAt(),
                    p.getApprovedAt(), p.getFailedAt(), p.getFailureCode(), p.getFailureMessage());
        }
    }
}
