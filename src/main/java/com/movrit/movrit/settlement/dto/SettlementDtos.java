package com.movrit.movrit.settlement.dto;

import com.movrit.movrit.settlement.entity.Settlement;
import com.movrit.movrit.settlement.entity.SettlementStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class SettlementDtos {
    private SettlementDtos() {}

    public record CreateRequest(
            @NotNull Long preorderId,
            @NotBlank String settlementNumber,
            @NotNull SettlementStatus status,
            @NotNull @DecimalMin("0") BigDecimal grossAmount,
            @NotNull @DecimalMin("0") BigDecimal refundAmount,
            @NotNull @DecimalMin("0") BigDecimal platformFeeAmount,
            @NotNull @DecimalMin("0") BigDecimal netAmount,
            @NotBlank String currency,
            LocalDateTime calculatedAt,
            LocalDateTime confirmedAt,
            LocalDateTime paidAt
    ) {}

    public record UpdateRequest(
            @NotNull SettlementStatus status,
            @NotNull @DecimalMin("0") BigDecimal grossAmount,
            @NotNull @DecimalMin("0") BigDecimal refundAmount,
            @NotNull @DecimalMin("0") BigDecimal platformFeeAmount,
            @NotNull @DecimalMin("0") BigDecimal netAmount,
            LocalDateTime calculatedAt,
            LocalDateTime confirmedAt,
            LocalDateTime paidAt
    ) {}

    public record Response(
            Long id,
            Long preorderId,
            String settlementNumber,
            SettlementStatus status,
            BigDecimal grossAmount,
            BigDecimal refundAmount,
            BigDecimal platformFeeAmount,
            BigDecimal netAmount,
            String currency
    ) {
        public static Response from(Settlement settlement) {
            return new Response(settlement.getId(), settlement.getPreorder().getId(), settlement.getSettlementNumber(),
                    settlement.getStatus(), settlement.getGrossAmount(), settlement.getRefundAmount(),
                    settlement.getPlatformFeeAmount(), settlement.getNetAmount(), settlement.getCurrency());
        }
    }
}
