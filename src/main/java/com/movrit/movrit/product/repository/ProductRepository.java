package com.movrit.movrit.product.repository;

import com.movrit.movrit.product.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 상품을 조회합니다. */
public interface ProductRepository extends JpaRepository<Product, Long> {
  /** 업체가 소유한 브랜드의 상품 중 삭제하지 않은 것을 최근 순으로 조회합니다. */
  List<Product> findAllByBrandSellerMemberIdAndDeletedAtIsNullOrderByIdDesc(Long memberId);
}
