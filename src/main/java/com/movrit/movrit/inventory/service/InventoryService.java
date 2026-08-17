package com.movrit.movrit.inventory.service;

import com.movrit.movrit.inventory.entity.Inventory;
import com.movrit.movrit.inventory.repository.InventoryRepository;
import com.movrit.movrit.product.entity.ProductSku;
import com.movrit.movrit.product.repository.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProductSkuRepository productSkuRepository;

    @Transactional
    public Inventory createForSku(Long skuId, long initialQuantity, long safetyStock) {
        if (inventoryRepository.findByProductSkuId(skuId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 재고가 생성된 SKU야.");
        }
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU를 찾을 수 없어."));
        if (initialQuantity < 0 || safetyStock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "재고 수량은 0 이상이어야 해.");
        }
        return inventoryRepository.save(Inventory.builder()
                .productSku(sku)
                .onHandQuantity(initialQuantity)
                .reservedQuantity(0L)
                .safetyStockQuantity(safetyStock)
                .build());
    }

    @Transactional
    public void receive(Long skuId, long quantity) {
        locked(skuId).receive(quantity);
    }

    @Transactional
    public void reserve(Long skuId, long quantity) {
        try {
            locked(skuId).reserve(quantity);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @Transactional
    public void release(Long skuId, long quantity) {
        locked(skuId).release(quantity);
    }

    @Transactional
    public void commit(Long skuId, long quantity) {
        try {
            locked(skuId).commit(quantity);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    public long available(Long skuId) {
        return inventoryRepository.findByProductSkuId(skuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재고 정보를 찾을 수 없어."))
                .availableQuantity();
    }

    private Inventory locked(Long skuId) {
        return inventoryRepository.findWithLockByProductSkuId(skuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재고 정보를 찾을 수 없어."));
    }
}
