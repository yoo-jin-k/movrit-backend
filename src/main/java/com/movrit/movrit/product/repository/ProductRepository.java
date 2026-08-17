package com.movrit.movrit.product.repository;

import com.movrit.movrit.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByBrandSellerMemberIdAndDeletedAtIsNullOrderByIdDesc(Long memberId);
}
