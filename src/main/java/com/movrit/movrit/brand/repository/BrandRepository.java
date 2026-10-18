package com.movrit.movrit.brand.repository;

import com.movrit.movrit.brand.entity.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
  List<Brand> findAllBySellerIdAndDeletedAtIsNull(Long sellerId);

  boolean existsBySellerIdAndNameIgnoreCaseAndDeletedAtIsNull(Long sellerId, String name);

  boolean existsBySellerIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
      Long sellerId, String name, Long id);
}
