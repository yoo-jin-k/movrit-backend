package com.movrit.movrit.shipping.repository;

import com.movrit.movrit.shipping.entity.Shipping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShippingRepository extends JpaRepository<Shipping, Long> {
    Optional<Shipping> findByOrderId(Long orderId);
    List<Shipping> findAllByOrderPreorderProductBrandSellerMemberIdOrderByIdDesc(Long memberId);
    List<Shipping> findAllByOrderMemberIdOrderByIdDesc(Long memberId);
}
