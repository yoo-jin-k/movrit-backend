package com.movrit.movrit.product.dto;

import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class ProductDtos {
    private ProductDtos() {}

    public record CreateRequest(
            @NotNull Long sellerId,
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

    public record Response(
            Long id,
            Long sellerId,
            String name,
            String description,
            String category,
            BigDecimal normalPrice,
            ProductStatus status
    ) {
        public static Response from(Product product) {
            return new Response(product.getId(), product.getSellerId(), product.getName(), product.getDescription(),
                    product.getCategory(), product.getNormalPrice(), product.getStatus());
        }
    }
}
