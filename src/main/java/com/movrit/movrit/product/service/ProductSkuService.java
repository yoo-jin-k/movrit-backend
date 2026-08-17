package com.movrit.movrit.product.service;

import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.entity.ProductSku;
import com.movrit.movrit.product.repository.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSkuService {
    private final ProductSkuRepository productSkuRepository;
    private final ProductService productService;

    @Transactional
    public ProductSku create(Long productId, String skuCode, String optionName, BigDecimal additionalPrice) {
        Product product = productService.getProduct(productId);
        String normalizedCode = skuCode.trim().toUpperCase();
        if (productSkuRepository.findBySkuCode(normalizedCode).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 SKU 코드야.");
        }
        return productSkuRepository.save(ProductSku.builder()
                .product(product)
                .skuCode(normalizedCode)
                .optionName(optionName.trim())
                .additionalPrice(additionalPrice)
                .active(true)
                .build());
    }

    public List<ProductSku> findActiveByProduct(Long productId) {
        return productSkuRepository.findAllByProductIdAndActiveTrue(productId);
    }

    public ProductSku get(Long id) {
        return productSkuRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU를 찾을 수 없어."));
    }
}
