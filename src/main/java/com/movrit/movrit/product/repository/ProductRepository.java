package com.movrit.movrit.product.repository;

import com.movrit.movrit.product.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
  List<Product> findAllByBrandSellerMemberIdAndDeletedAtIsNullOrderByIdDesc(Long memberId);
}
