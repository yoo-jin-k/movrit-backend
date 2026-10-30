package com.movrit.movrit.settlement.dto;

import com.movrit.movrit.settlement.entity.Settlement;
import com.movrit.movrit.settlement.entity.SettlementStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 정산 기록 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
public final class SettlementDtos {
  private SettlementDtos() {}

  /**
   * 정산 기록의 생성 요청을 전달합니다.
   *
   * @param preorderId 연결된 프리오더 식별자입니다. null을 허용하지 않습니다.
   * @param platformFeeRate 플랫폼 수수료율이며 허용 범위는 0부터 100까지입니다. null을 허용하지 않습니다.
   */
  public record CreateRequest(
      @NotNull Long preorderId,
      @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal platformFeeRate) {}

  /**
   * 정산 기록의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param preorderId 연결된 프리오더 식별자입니다.
   * @param settlementNumber 정산 표시 번호입니다.
   * @param status 현재 업무 상태입니다.
   * @param grossAmount 정산 기준 총금액입니다.
   * @param refundAmount 환불 금액입니다.
   * @param platformFeeAmount 플랫폼 수수료 금액입니다.
   * @param netAmount 최종 정산 금액입니다.
   * @param currency 금액의 통화 코드입니다.
   */
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
    /** 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
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
