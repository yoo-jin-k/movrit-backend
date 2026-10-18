package com.movrit.movrit.product.dto;

import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.entity.ProductStatus;
import com.movrit.movrit.product.entity.ProductVariant;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 상품 및 옵션의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class ProductDtos {
  private ProductDtos() {}

  /**
   * 상품 및 옵션의 생성 요청을 전달합니다.
   *
   * @param brandId 브랜드 식별자입니다. null을 허용하지 않습니다.
   * @param name 표시 이름입니다. 빈 문자열을 허용하지 않습니다.
   * @param description 상세 설명입니다.
   * @param category 분류값입니다.
   * @param normalPrice 상품 정상 가격입니다. null을 허용하지 않습니다.
   * @param status 현재 업무 상태입니다. null을 허용하지 않습니다.
   */
  public record CreateRequest(
      @NotNull Long brandId,
      @NotBlank String name,
      String description,
      String category,
      @NotNull @DecimalMin("0") BigDecimal normalPrice,
      @NotNull ProductStatus status) {}

  /**
   * 상품 및 옵션의 수정 요청을 전달합니다.
   *
   * @param name 표시 이름입니다. 빈 문자열을 허용하지 않습니다.
   * @param description 상세 설명입니다.
   * @param category 분류값입니다.
   * @param normalPrice 상품 정상 가격입니다. null을 허용하지 않습니다.
   * @param status 현재 업무 상태입니다. null을 허용하지 않습니다.
   */
  public record UpdateRequest(
      @NotBlank String name,
      String description,
      String category,
      @NotNull @DecimalMin("0") BigDecimal normalPrice,
      @NotNull ProductStatus status) {}

  /**
   * 상품 및 옵션의 상품 옵션 생성 요청을 전달합니다.
   *
   * @param skuCode 상품 옵션의 재고 관리 코드입니다. 빈 문자열을 허용하지 않습니다.
   * @param optionName1 첫 번째 옵션 이름입니다.
   * @param optionValue1 첫 번째 옵션 값입니다.
   * @param optionName2 두 번째 옵션 이름입니다.
   * @param optionValue2 두 번째 옵션 값입니다.
   * @param additionalPrice 옵션 추가 가격입니다. null을 허용하지 않습니다.
   * @param initialStock 초기 재고 수량입니다. null을 허용하지 않습니다. 최솟값은 0입니다.
   * @param safetyStock 설정할 안전 재고 수량입니다. null을 허용하지 않습니다. 최솟값은 0입니다.
   */
  public record VariantCreateRequest(
      @NotBlank String skuCode,
      String optionName1,
      String optionValue1,
      String optionName2,
      String optionValue2,
      @NotNull @DecimalMin("0") BigDecimal additionalPrice,
      @NotNull @Min(0) Long initialStock,
      @NotNull @Min(0) Long safetyStock) {}

  /**
   * 상품 및 옵션의 상품 옵션 수정 요청을 전달합니다.
   *
   * @param optionName1 첫 번째 옵션 이름입니다.
   * @param optionValue1 첫 번째 옵션 값입니다.
   * @param optionName2 두 번째 옵션 이름입니다.
   * @param optionValue2 두 번째 옵션 값입니다.
   * @param additionalPrice 옵션 추가 가격입니다. null을 허용하지 않습니다.
   * @param active 옵션 활성 여부입니다.
   */
  public record VariantUpdateRequest(
      String optionName1,
      String optionValue1,
      String optionName2,
      String optionValue2,
      @NotNull @DecimalMin("0") BigDecimal additionalPrice,
      boolean active) {}

  /**
   * 상품 및 옵션의 재고 입고 요청을 전달합니다.
   *
   * @param quantity 수량입니다. null을 허용하지 않습니다. 최솟값은 1입니다.
   */
  public record StockRequest(@NotNull @Min(1) Long quantity) {}

  /**
   * 상품 및 옵션의 안전 재고 수정 요청을 전달합니다.
   *
   * @param quantity 수량입니다. null을 허용하지 않습니다. 최솟값은 0입니다.
   */
  public record SafetyStockRequest(@NotNull @Min(0) Long quantity) {}

  /**
   * 상품 및 옵션의 상품 옵션 및 재고 응답을 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param skuCode 상품 옵션의 재고 관리 코드입니다.
   * @param optionName1 첫 번째 옵션 이름입니다.
   * @param optionValue1 첫 번째 옵션 값입니다.
   * @param optionName2 두 번째 옵션 이름입니다.
   * @param optionValue2 두 번째 옵션 값입니다.
   * @param additionalPrice 옵션 추가 가격입니다.
   * @param onHandQuantity 보유 재고 수량입니다.
   * @param reservedQuantity 주문에 예약된 재고 수량입니다.
   * @param safetyStockQuantity 안전 재고 수량입니다.
   * @param soldQuantity 판매 수량입니다.
   * @param availableQuantity 현재 판매 가능 수량입니다.
   * @param active 옵션 활성 여부입니다.
   */
  public record VariantResponse(
      Long id,
      String skuCode,
      String optionName1,
      String optionValue1,
      String optionName2,
      String optionValue2,
      BigDecimal additionalPrice,
      Long onHandQuantity,
      Long reservedQuantity,
      Long safetyStockQuantity,
      Long soldQuantity,
      Long availableQuantity,
      boolean active) {
    /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
    public static VariantResponse from(ProductVariant variant) {
      return new VariantResponse(
          variant.getId(),
          variant.getSkuCode(),
          variant.getOptionName1(),
          variant.getOptionValue1(),
          variant.getOptionName2(),
          variant.getOptionValue2(),
          variant.getAdditionalPrice(),
          variant.getOnHandQuantity(),
          variant.getReservedQuantity(),
          variant.getSafetyStockQuantity(),
          variant.getSoldQuantity(),
          variant.getAvailableQuantity(),
          variant.isActive());
    }
  }

  /**
   * 상품 및 옵션의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param brandId 브랜드 식별자입니다.
   * @param brandName 브랜드명입니다.
   * @param name 표시 이름입니다.
   * @param description 상세 설명입니다.
   * @param category 분류값입니다.
   * @param normalPrice 상품 정상 가격입니다.
   * @param thumbnailUrl 상품 대표 이미지 주소입니다.
   * @param status 현재 업무 상태입니다.
   */
  public record Response(
      Long id,
      Long brandId,
      String brandName,
      String name,
      String description,
      String category,
      BigDecimal normalPrice,
      String thumbnailUrl,
      ProductStatus status) {
    /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
    public static Response from(Product product) {
      return new Response(
          product.getId(),
          product.getBrand().getId(),
          product.getBrand().getName(),
          product.getName(),
          product.getDescription(),
          product.getCategory(),
          product.getNormalPrice(),
          product.getThumbnailUrl(),
          product.getStatus());
    }
  }
}
