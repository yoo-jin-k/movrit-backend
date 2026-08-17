package com.movrit.movrit.product.dto;

import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.entity.ProductStatus;
import com.movrit.movrit.product.entity.ProductVariant;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class ProductDtos {
    private ProductDtos() {}

    public record CreateRequest(
            @NotNull Long brandId,
            @NotBlank String name,
            String description,
            String category,
            @NotNull @DecimalMin("0") BigDecimal normalPrice,
            @NotNull ProductStatus status
    ) {}

    public record UpdateRequest(
            @NotBlank String name,
            String description,
            String category,
            @NotNull @DecimalMin("0") BigDecimal normalPrice,
            @NotNull ProductStatus status
    ) {}

    public record VariantCreateRequest(
            @NotBlank String skuCode,
            String optionName1,
            String optionValue1,
            String optionName2,
            String optionValue2,
            @NotNull @DecimalMin("0") BigDecimal additionalPrice,
            @NotNull @Min(0) Long initialStock,
            @NotNull @Min(0) Long safetyStock
    ) {}

    public record VariantUpdateRequest(
            String optionName1,
            String optionValue1,
            String optionName2,
            String optionValue2,
            @NotNull @DecimalMin("0") BigDecimal additionalPrice,
            boolean active
    ) {}

    public record StockRequest(@NotNull @Min(1) Long quantity) {}
    public record SafetyStockRequest(@NotNull @Min(0) Long quantity) {}

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
            boolean active
    ) {
        public static VariantResponse from(ProductVariant variant) {
            return new VariantResponse(
                    variant.getId(), variant.getSkuCode(),
                    variant.getOptionName1(), variant.getOptionValue1(),
                    variant.getOptionName2(), variant.getOptionValue2(),
                    variant.getAdditionalPrice(), variant.getOnHandQuantity(),
                    variant.getReservedQuantity(), variant.getSafetyStockQuantity(),
                    variant.getSoldQuantity(), variant.getAvailableQuantity(), variant.isActive()
            );
        }
    }

    public record Response(
            Long id,
            Long brandId,
            String brandName,
            String name,
            String description,
            String category,
            BigDecimal normalPrice,
            String thumbnailUrl,
            ProductStatus status
    ) {
        public static Response from(Product product) {
            return new Response(
                    product.getId(), product.getBrand().getId(), product.getBrand().getName(),
                    product.getName(), product.getDescription(), product.getCategory(),
                    product.getNormalPrice(), product.getThumbnailUrl(), product.getStatus()
            );
        }
    }
}
