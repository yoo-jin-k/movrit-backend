package com.movrit.movrit.order.repository;

import com.movrit.movrit.order.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
  Optional<Order> findByRequestKey(String requestKey);

  List<Order> findAllByMemberIdOrderByIdDesc(Long memberId);

  List<Order> findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(Long memberId);

  long countByMemberIdAndPreorderIdAndStatusNot(
      Long memberId, Long preorderId, com.movrit.movrit.order.entity.OrderStatus status);

  List<Order> findAllByAttributionId(Long attributionId);

  List<Order> findAllByPreorderId(Long preorderId);
}
