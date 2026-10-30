package com.movrit.movrit.brand.repository;

import com.movrit.movrit.brand.entity.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 업체가 소유한 브랜드를 조회합니다. 삭제(deletedAt 설정)된 브랜드는 제외합니다. */
public interface BrandRepository extends JpaRepository<Brand, Long> {
  List<Brand> findAllBySellerIdAndDeletedAtIsNull(Long sellerId);

  /** 같은 업체 안에서 대소문자 구분 없이 같은 이름의 사용 중 브랜드가 있는지 확인합니다. */
  boolean existsBySellerIdAndNameIgnoreCaseAndDeletedAtIsNull(Long sellerId, String name);

  /** 브랜드 이름을 바꿀 때 자기 자신을 제외하고 같은 이름이 있는지 확인합니다. */
  boolean existsBySellerIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
      Long sellerId, String name, Long id);
}
