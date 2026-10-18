package com.movrit.movrit.shipping.dto;

import com.movrit.movrit.shipping.entity.Shipping;
import com.movrit.movrit.shipping.entity.ShippingStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** 배송의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class ShippingDtos {
  private ShippingDtos() {}

  /**
   * 배송의 생성 요청을 전달합니다.
   *
   * @param orderId 주문 식별자입니다. null을 허용하지 않습니다.
   * @param courierCode 택배사 코드입니다.
   * @param trackingNumber 운송장 번호입니다.
   * @param status 현재 업무 상태입니다. null을 허용하지 않습니다.
   * @param shippedAt 발송 시각입니다.
   * @param deliveredAt 배송 완료 시각입니다.
   */
  public record CreateRequest(
      @NotNull Long orderId,
      String courierCode,
      String trackingNumber,
      @NotNull ShippingStatus status,
      LocalDateTime shippedAt,
      LocalDateTime deliveredAt) {}

  /**
   * 배송의 수정 요청을 전달합니다.
   *
   * @param courierCode 택배사 코드입니다.
   * @param trackingNumber 운송장 번호입니다.
   * @param status 현재 업무 상태입니다. null을 허용하지 않습니다.
   * @param shippedAt 발송 시각입니다.
   * @param deliveredAt 배송 완료 시각입니다.
   */
  public record UpdateRequest(
      String courierCode,
      String trackingNumber,
      @NotNull ShippingStatus status,
      LocalDateTime shippedAt,
      LocalDateTime deliveredAt) {}

  /**
   * 배송의 발송 정보 입력 요청을 전달합니다.
   *
   * @param courierCode 택배사 코드입니다. null을 허용하지 않습니다.
   * @param trackingNumber 운송장 번호입니다. null을 허용하지 않습니다.
   */
  public record ShipRequest(@NotNull String courierCode, @NotNull String trackingNumber) {}

  /**
   * 배송의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param orderId 주문 식별자입니다.
   * @param courierCode 택배사 코드입니다.
   * @param trackingNumber 운송장 번호입니다.
   * @param status 현재 업무 상태입니다.
   * @param shippedAt 발송 시각입니다.
   * @param deliveredAt 배송 완료 시각입니다.
   */
  public record Response(
      Long id,
      Long orderId,
      String courierCode,
      String trackingNumber,
      ShippingStatus status,
      LocalDateTime shippedAt,
      LocalDateTime deliveredAt) {
    /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
    public static Response from(Shipping shipping) {
      return new Response(
          shipping.getId(),
          shipping.getOrder().getId(),
          shipping.getCourierCode(),
          shipping.getTrackingNumber(),
          shipping.getStatus(),
          shipping.getShippedAt(),
          shipping.getDeliveredAt());
    }
  }
}
