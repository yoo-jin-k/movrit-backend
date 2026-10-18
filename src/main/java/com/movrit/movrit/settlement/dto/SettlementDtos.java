package com.movrit.movrit.settlement.dto;

import com.movrit.movrit.settlement.entity.Settlement;
import com.movrit.movrit.settlement.entity.SettlementStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class SettlementDtos {
  private SettlementDtos() {}

  public record CreateRequest(
      @NotNull Long preorderId,
      @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal platformFeeRate) {}

  public record Response(
      Long id,
      Long preorderId,
      String settlementNumber,
      SettlementStatus status,
      BigDecimal grossAmount,
      BigDecimal refundAmount,
      BigDecimal platformFeeAmount,
      BigDecimal netAmount,
      String currency) {
    public static Response from(Settlement settlement) {
      return new Response(
          settlement.getId(),
          settlement.getPreorder().getId(),
          settlement.getSettlementNumber(),
          settlement.getStatus(),
          settlement.getGrossAmount(),
          settlement.getRefundAmount(),
          settlement.getPlatformFeeAmount(),
          settlement.getNetAmount(),
          settlement.getCurrency());
    }
  }
}
