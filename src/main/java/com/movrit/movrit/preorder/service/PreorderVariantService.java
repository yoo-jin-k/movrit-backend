package com.movrit.movrit.preorder.service;

import com.movrit.movrit.inventory.service.InventoryService;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.entity.PreorderVariant;
import com.movrit.movrit.preorder.repository.PreorderVariantRepository;
import com.movrit.movrit.product.entity.ProductSku;
import com.movrit.movrit.product.service.ProductSkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreorderVariantService {
    private final PreorderVariantRepository repository;
    private final PreorderService preorderService;
    private final ProductSkuService productSkuService;
    private final InventoryService inventoryService;

    @Transactional
    public PreorderVariant create(Long preorderId, Long skuId, BigDecimal salePrice, Long maxQuantity) {
        Preorder preorder = preorderService.getPreorder(preorderId);
        ProductSku sku = productSkuService.get(skuId);
        if (!sku.getProduct().getId().equals(preorder.getProduct().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프리오더 상품과 SKU 상품이 달라.");
        }
        if (repository.existsByPreorderIdAndProductSkuId(preorderId, skuId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 프리오더에 연결된 SKU야.");
        }
        return repository.save(PreorderVariant.builder()
                .preorder(preorder)
                .productSku(sku)
                .salePrice(salePrice)
                .maxQuantity(maxQuantity)
                .orderedQuantity(0L)
                .active(true)
                .build());
    }

    public List<PreorderVariant> findAll(Long preorderId) {
        return repository.findAllByPreorderIdAndActiveTrue(preorderId);
    }

    public PreorderVariant get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더 옵션을 찾을 수 없어."));
    }

    @Transactional
    public PreorderVariant reserve(Long id, long quantity) {
        PreorderVariant variant = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더 옵션을 찾을 수 없어."));
        validateSellable(variant.getPreorder());
        try {
            variant.reserve(quantity);
            inventoryService.reserve(variant.getProductSku().getId(), quantity);
            variant.getPreorder().increaseOrderedQuantity(quantity);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return variant;
    }

    @Transactional
    public void release(Long id, long quantity) {
        PreorderVariant variant = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더 옵션을 찾을 수 없어."));
        variant.release(quantity);
        inventoryService.release(variant.getProductSku().getId(), quantity);
        variant.getPreorder().decreaseOrderedQuantity(quantity);
    }

    @Transactional
    public void commit(Long id, long quantity) {
        PreorderVariant variant = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더 옵션을 찾을 수 없어."));
        inventoryService.commit(variant.getProductSku().getId(), quantity);
    }

    private void validateSellable(Preorder preorder) {
        LocalDateTime now = LocalDateTime.now();
        if (preorder.getStatus() != PreorderStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 판매 중인 공동구매가 아니야.");
        }
        if (now.isBefore(preorder.getStartAt()) || now.isAfter(preorder.getEndAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "공동구매 판매기간이 아니야.");
        }
        if (preorder.getMaxQuantity() != null && preorder.getOrderedQuantity() >= preorder.getMaxQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "공동구매 최대 판매수량에 도달했어.");
        }
    }
}
