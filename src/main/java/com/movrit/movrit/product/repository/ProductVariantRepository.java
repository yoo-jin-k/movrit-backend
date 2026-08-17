package com.movrit.movrit.product.repository;

import com.movrit.movrit.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findAllByProductIdOrderByIdAsc(Long productId);
    boolean existsBySkuCode(String skuCode);
}
