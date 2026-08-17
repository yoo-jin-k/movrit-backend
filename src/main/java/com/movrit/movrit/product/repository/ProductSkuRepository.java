package com.movrit.movrit.product.repository;

import com.movrit.movrit.product.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    Optional<ProductSku> findBySkuCode(String skuCode);
    List<ProductSku> findAllByProductIdAndActiveTrue(Long productId);
}
