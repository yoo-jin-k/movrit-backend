package com.movrit.movrit.brand.repository;

import com.movrit.movrit.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findAllBySellerIdAndDeletedAtIsNull(Long sellerId);
    boolean existsBySellerIdAndNameIgnoreCaseAndDeletedAtIsNull(Long sellerId, String name);
}
