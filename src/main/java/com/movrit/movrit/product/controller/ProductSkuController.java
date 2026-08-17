package com.movrit.movrit.product.controller;

import com.movrit.movrit.product.entity.ProductSku;
import com.movrit.movrit.product.service.ProductSkuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/{productId}/skus")
public class ProductSkuController {
    private final ProductSkuService productSkuService;

    public record CreateRequest(@NotBlank String skuCode, @NotBlank String optionName,
                                @NotNull @DecimalMin("0") BigDecimal additionalPrice) {}
    public record Response(Long id, Long productId, String skuCode, String optionName, BigDecimal additionalPrice, boolean active) {
        static Response from(ProductSku sku) {
            return new Response(sku.getId(), sku.getProduct().getId(), sku.getSkuCode(), sku.getOptionName(), sku.getAdditionalPrice(), sku.isActive());
        }
    }

    @PostMapping
    public ResponseEntity<Response> create(@PathVariable Long productId, @Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Response.from(productSkuService.create(
                productId, request.skuCode(), request.optionName(), request.additionalPrice())));
    }

    @GetMapping
    public List<Response> findAll(@PathVariable Long productId) {
        return productSkuService.findActiveByProduct(productId).stream().map(Response::from).toList();
    }
}
