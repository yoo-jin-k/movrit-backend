package com.movrit.movrit.preorder.controller;

import com.movrit.movrit.preorder.entity.PreorderVariant;
import com.movrit.movrit.preorder.service.PreorderVariantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/preorders/{preorderId}/variants")
public class PreorderVariantController {
    private final PreorderVariantService service;

    public record CreateRequest(@NotNull Long skuId, @NotNull @DecimalMin("0") BigDecimal salePrice,
                                @Min(1) Long maxQuantity) {}
    public record Response(Long id, Long preorderId, Long skuId, String optionName, BigDecimal salePrice,
                           Long maxQuantity, Long orderedQuantity, long remainingQuantity) {
        static Response from(PreorderVariant variant) {
            return new Response(variant.getId(), variant.getPreorder().getId(), variant.getProductSku().getId(),
                    variant.getProductSku().getOptionName(), variant.getSalePrice(), variant.getMaxQuantity(),
                    variant.getOrderedQuantity(), variant.remainingQuantity());
        }
    }

    @PostMapping
    public ResponseEntity<Response> create(@PathVariable Long preorderId, @Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Response.from(
                service.create(preorderId, request.skuId(), request.salePrice(), request.maxQuantity())));
    }

    @GetMapping
    public List<Response> findAll(@PathVariable Long preorderId) {
        return service.findAll(preorderId).stream().map(Response::from).toList();
    }
}
