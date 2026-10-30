package com.movrit.movrit.shipping.repository;

import com.movrit.movrit.shipping.entity.Shipping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 주문 배송 정보를 조회합니다. */
public interface ShippingRepository extends JpaRepository<Shipping, Long> {
  /** 주문의 배송 정보를 찾습니다. 주문 한 건에는 배송 정보가 하나만 있습니다. */
  Optional<Shipping> findByOrderId(Long orderId);

  /** 업체가 소유한 브랜드 주문의 배송 목록을 최근 순으로 조회합니다. */
  List<Shipping> findAllByOrderPreorderProductBrandSellerMemberIdOrderByIdDesc(Long memberId);

  /** 구매자 본인 주문의 배송 목록을 최근 순으로 조회합니다. */
  List<Shipping> findAllByOrderMemberIdOrderByIdDesc(Long memberId);
}
