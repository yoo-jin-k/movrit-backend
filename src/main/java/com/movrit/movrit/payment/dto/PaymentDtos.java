package com.movrit.movrit.payment.dto;

import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 결제 기록의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class PaymentDtos {
  private PaymentDtos() {}

  /**
   * 결제 기록의 생성 요청을 전달합니다.
   *
   * @param orderId 주문 식별자입니다. null을 허용하지 않습니다.
   * @param idempotencyKey 동일 결제 요청의 중복 처리를 방지하는 식별값입니다. 빈 문자열을 허용하지 않습니다.
   */
  public record CreateRequest(@NotNull Long orderId, @NotBlank String idempotencyKey) {}

  /**
   * 결제 기록의 승인 요청을 전달합니다.
   *
   * @param paymentKey 결제 식별값입니다. 빈 문자열을 허용하지 않습니다.
   * @param pgProvider 결제 제공자 코드입니다. 빈 문자열을 허용하지 않습니다.
   * @param method 결제 수단입니다. 빈 문자열을 허용하지 않습니다.
   */
  public record ApproveRequest(
      @NotBlank String paymentKey, @NotBlank String pgProvider, @NotBlank String method) {}

  /**
   * 결제 기록의 결제 실패 기록 요청을 전달합니다.
   *
   * @param failureCode 실패 코드입니다. 빈 문자열을 허용하지 않습니다.
   * @param failureMessage 실패 설명입니다. 빈 문자열을 허용하지 않습니다.
   */
  public record FailRequest(@NotBlank String failureCode, @NotBlank String failureMessage) {}

  /**
   * 결제 기록의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param orderId 주문 식별자입니다.
   * @param paymentKey 결제 식별값입니다.
   * @param pgProvider 결제 제공자 코드입니다.
   * @param method 결제 수단입니다.
   * @param status 현재 업무 상태입니다.
   * @param requestedAmount 요청 금액입니다.
   * @param approvedAmount 승인 금액입니다.
   * @param currency 금액의 통화 코드입니다.
   * @param requestedAt 요청 시각입니다.
   * @param approvedAt 승인 시각입니다.
   * @param failedAt 실패 시각입니다.
   * @param failureCode 실패 코드입니다.
   * @param failureMessage 실패 설명입니다.
   */
  public record Response(
      Long id,
      Long orderId,
      String paymentKey,
      String pgProvider,
      String method,
      PaymentStatus status,
      BigDecimal requestedAmount,
      BigDecimal approvedAmount,
      String currency,
      LocalDateTime requestedAt,
      LocalDateTime approvedAt,
      LocalDateTime failedAt,
      String failureCode,
      String failureMessage) {
    /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
    public static Response from(Payment p) {
      return new Response(
          p.getId(),
          p.getOrder().getId(),
          p.getPaymentKey(),
          p.getPgProvider(),
          p.getMethod(),
          p.getStatus(),
          p.getRequestedAmount(),
          p.getApprovedAmount(),
          p.getCurrency(),
          p.getRequestedAt(),
          p.getApprovedAt(),
          p.getFailedAt(),
          p.getFailureCode(),
          p.getFailureMessage());
    }
  }
}
