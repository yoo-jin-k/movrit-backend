package com.movrit.movrit.preorder.dto;

import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 프리오더 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
public final class PreorderDtos {
  private PreorderDtos() {}

  /**
   * 프리오더의 생성 요청을 전달합니다.
   *
   * @param productId 상품 식별자입니다. null을 허용하지 않습니다.
   * @param variantIds 판매 가능한 상품 옵션 식별자 목록이며 기존 가격·주문 참조와 구분합니다. null을 허용하지 않습니다. 빈 목록을 허용하지 않습니다.
   * @param title 제목입니다. 빈 문자열을 허용하지 않습니다.
   * @param description 상세 설명입니다.
   * @param preorderPrice 프리오더 판매 가격입니다. null을 허용하지 않습니다.
   * @param currency 금액의 통화 코드입니다. 빈 문자열을 허용하지 않습니다.
   * @param targetQuantity 목표 수량입니다. null을 허용하지 않습니다. 입력하는 경우 양수여야 합니다.
   * @param maxQuantity 최대 판매 수량입니다. 입력하는 경우 양수여야 합니다.
   * @param minOrderQuantity 최소 주문 수량입니다. null을 허용하지 않습니다. 최솟값은 1입니다.
   * @param maxOrderQuantityPerMember 회원별 최대 주문 수량입니다. 최솟값은 1입니다.
   * @param startAt 시작 시각입니다. null을 허용하지 않습니다.
   * @param endAt 종료 시각입니다. null을 허용하지 않습니다.
   * @param productionExpectedAt 예상 생산 시각입니다.
   * @param shippingExpectedAt 예상 배송 시각입니다.
   */
  public record CreateRequest(
      @NotNull Long productId,
      @NotEmpty List<@NotNull Long> variantIds,
      @NotBlank String title,
      String description,
      @NotNull @DecimalMin("0") BigDecimal preorderPrice,
      @NotBlank String currency,
      @NotNull @Positive Long targetQuantity,
      @Positive Long maxQuantity,
      @NotNull @Min(1) Long minOrderQuantity,
      @Min(1) Long maxOrderQuantityPerMember,
      @NotNull LocalDateTime startAt,
      @NotNull LocalDateTime endAt,
      LocalDateTime productionExpectedAt,
      LocalDateTime shippingExpectedAt) {}

  /**
   * 프리오더의 수정 요청을 전달합니다.
   *
   * @param variantIds 판매 가능한 상품 옵션 식별자 목록이며 기존 가격·주문 참조와 구분합니다. null을 허용하지 않습니다. 빈 목록을 허용하지 않습니다.
   * @param title 제목입니다. 빈 문자열을 허용하지 않습니다.
   * @param description 상세 설명입니다.
   * @param preorderPrice 프리오더 판매 가격입니다. null을 허용하지 않습니다.
   * @param currency 금액의 통화 코드입니다. 빈 문자열을 허용하지 않습니다.
   * @param targetQuantity 목표 수량입니다. null을 허용하지 않습니다. 입력하는 경우 양수여야 합니다.
   * @param maxQuantity 최대 판매 수량입니다. 입력하는 경우 양수여야 합니다.
   * @param minOrderQuantity 최소 주문 수량입니다. null을 허용하지 않습니다. 최솟값은 1입니다.
   * @param maxOrderQuantityPerMember 회원별 최대 주문 수량입니다. 최솟값은 1입니다.
   * @param startAt 시작 시각입니다. null을 허용하지 않습니다.
   * @param endAt 종료 시각입니다. null을 허용하지 않습니다.
   * @param productionExpectedAt 예상 생산 시각입니다.
   * @param shippingExpectedAt 예상 배송 시각입니다.
   */
  public record UpdateRequest(
      @NotEmpty List<@NotNull Long> variantIds,
      @NotBlank String title,
      String description,
      @NotNull @DecimalMin("0") BigDecimal preorderPrice,
      @NotBlank String currency,
      @NotNull @Positive Long targetQuantity,
      @Positive Long maxQuantity,
      @NotNull @Min(1) Long minOrderQuantity,
      @Min(1) Long maxOrderQuantityPerMember,
      @NotNull LocalDateTime startAt,
      @NotNull LocalDateTime endAt,
      LocalDateTime productionExpectedAt,
      LocalDateTime shippingExpectedAt) {}

  /**
   * 프리오더의 취소 요청을 전달합니다.
   *
   * @param reason 처리 또는 판정 사유입니다. 빈 문자열을 허용하지 않습니다.
   */
  public record CancelRequest(@NotBlank String reason) {}

  /**
   * 프리오더의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param brandId 브랜드 식별자입니다.
   * @param productId 상품 식별자입니다.
   * @param productName 상품명입니다.
   * @param brandName 브랜드명입니다.
   * @param category 분류값입니다.
   * @param variantIds 판매 가능한 상품 옵션 식별자 목록이며 기존 가격·주문 참조와 구분합니다.
   * @param title 제목입니다.
   * @param description 상세 설명입니다.
   * @param preorderPrice 프리오더 판매 가격입니다.
   * @param currency 금액의 통화 코드입니다.
   * @param targetQuantity 목표 수량입니다.
   * @param maxQuantity 최대 판매 수량입니다.
   * @param orderedQuantity 누적 주문 수량입니다.
   * @param achievementRate 목표 대비 달성률입니다.
   * @param remainingSeconds 종료까지 남은 초이며 음수는 0으로 반환합니다.
   * @param minOrderQuantity 최소 주문 수량입니다.
   * @param maxOrderQuantityPerMember 회원별 최대 주문 수량입니다.
   * @param startAt 시작 시각입니다.
   * @param endAt 종료 시각입니다.
   * @param productionExpectedAt 예상 생산 시각입니다.
   * @param shippingExpectedAt 예상 배송 시각입니다.
   * @param status 현재 업무 상태입니다.
   * @param cancellationReason 취소 사유입니다.
   */
  public record Response(
      Long id,
      Long brandId,
      Long productId,
      String productName,
      String brandName,
      String category,
      List<Long> variantIds,
      String title,
      String description,
      BigDecimal preorderPrice,
      String currency,
      Long targetQuantity,
      Long maxQuantity,
      Long orderedQuantity,
      double achievementRate,
      long remainingSeconds,
      Long minOrderQuantity,
      Long maxOrderQuantityPerMember,
      LocalDateTime startAt,
      LocalDateTime endAt,
      LocalDateTime productionExpectedAt,
      LocalDateTime shippingExpectedAt,
      PreorderStatus status,
      String cancellationReason) {
    /**
     * 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. 판매 옵션 ID를 정렬하고 달성률은 최대 100%, 남은 시간은
     * 최소 0초로 계산합니다.
     */
    public static Response from(Preorder preorder) {
      return new Response(
          preorder.getId(),
          preorder.getProduct().getBrand().getId(),
          preorder.getProduct().getId(),
          preorder.getProduct().getName(),
          preorder.getProduct().getBrand().getName(),
          preorder.getProduct().getCategory(),
          preorder.getSaleVariants().stream().map(variant -> variant.getId()).sorted().toList(),
          preorder.getTitle(),
          preorder.getDescription(),
          preorder.getPreorderPrice(),
          preorder.getCurrency(),
          preorder.getTargetQuantity(),
          preorder.getMaxQuantity(),
          preorder.getOrderedQuantity(),
          Math.min(100.0, preorder.getOrderedQuantity() * 100.0 / preorder.getTargetQuantity()),
          Math.max(
              0, java.time.Duration.between(LocalDateTime.now(), preorder.getEndAt()).getSeconds()),
          preorder.getMinOrderQuantity(),
          preorder.getMaxOrderQuantityPerMember(),
          preorder.getStartAt(),
          preorder.getEndAt(),
          preorder.getProductionExpectedAt(),
          preorder.getShippingExpectedAt(),
          preorder.getStatus(),
          preorder.getCancellationReason());
    }
  }
}
