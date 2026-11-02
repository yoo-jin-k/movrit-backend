package com.movrit.movrit.campaignshipping.dto;

import com.movrit.movrit.campaignshipping.entity.CampaignShipment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** 배송 관리 API의 요청·응답 형식입니다. */
public final class CampaignShippingDtos {
  private CampaignShippingDtos() {}

  /**
   * 업체 배송 관리 목록 한 줄입니다.
   *
   * @param receivedAt 참여자가 수령(체험 완료)을 확인한 시각
   */
  public record Row(
      Long applicationId,
      String creatorNickname,
      String applicationStatus,
      String recipientName,
      String recipientPhone,
      String postalCode,
      String address1,
      String address2,
      String courier,
      String trackingNumber,
      LocalDateTime shippedAt,
      LocalDateTime receivedAt) {}

  /** 송장 입력 한 건입니다. */
  public record ShipmentInput(@NotNull Long applicationId, String courier, String trackingNumber) {}

  /** 여러 참여자의 송장을 한 번에 저장합니다(최대 100건). */
  public record SaveRequest(@NotEmpty @Size(max = 100) List<@Valid ShipmentInput> shipments) {}

  /** 건별 저장 결과입니다. 실패하면 message에 사유가 담깁니다. */
  public record SaveResult(Long applicationId, boolean success, String message) {}

  public record SaveResponse(int successCount, int failureCount, List<SaveResult> results) {}

  /** 참여자·업체가 보는 발송 정보입니다. */
  public record Shipment(
      Long applicationId,
      String courier,
      String trackingNumber,
      LocalDateTime shippedAt,
      LocalDateTime updatedAt) {
    public static Shipment from(CampaignShipment value) {
      return new Shipment(
          value.getApplicationId(),
          value.getCourier(),
          value.getTrackingNumber(),
          value.getShippedAt(),
          value.getUpdatedAt());
    }
  }
}
