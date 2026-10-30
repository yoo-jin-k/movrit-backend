package com.movrit.movrit.product.repository;

import com.movrit.movrit.product.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** 상품 옵션(SKU)과 재고를 조회합니다. */
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
  /** 옵션을 쓰기 잠금으로 조회합니다. 동시에 들어온 주문이 같은 재고를 함께 예약하지 못하게 합니다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from ProductVariant v where v.id = :id")
  Optional<ProductVariant> findLockedById(Long id);

  /** 상품의 옵션 목록을 등록 순으로 조회합니다. */
  List<ProductVariant> findAllByProductIdOrderByIdAsc(Long productId);

  /** 같은 SKU 코드가 이미 있는지 확인합니다. */
  boolean existsBySkuCode(String skuCode);
}
