package com.movrit.movrit.inventory.controller;

import com.movrit.movrit.inventory.entity.Inventory;
import com.movrit.movrit.inventory.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class InventoryController {
    private final InventoryService inventoryService;

    public record CreateRequest(@NotNull Long skuId, @NotNull @Min(0) Long initialQuantity, @NotNull @Min(0) Long safetyStock) {}
    public record QuantityRequest(@NotNull @Min(1) Long quantity) {}
    public record Response(Long skuId, long onHand, long reserved, long safetyStock, long available) {
        static Response from(Inventory inventory) {
            return new Response(inventory.getProductSku().getId(), inventory.getOnHandQuantity(),
                    inventory.getReservedQuantity(), inventory.getSafetyStockQuantity(), inventory.availableQuantity());
        }
    }

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Response.from(inventoryService.createForSku(request.skuId(), request.initialQuantity(), request.safetyStock())));
    }

    @PostMapping("/{skuId}/receive")
    public ResponseEntity<Void> receive(@PathVariable Long skuId, @Valid @RequestBody QuantityRequest request) {
        inventoryService.receive(skuId, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{skuId}/available")
    public long available(@PathVariable Long skuId) {
        return inventoryService.available(skuId);
    }
}
