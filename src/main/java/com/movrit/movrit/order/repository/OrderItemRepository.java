package com.movrit.movrit.order.repository;

import com.movrit.movrit.order.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 주문에 담긴 상품(옵션·수량·금액) 항목을 조회합니다. */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  /** 주문 한 건에 담긴 항목을 모두 조회합니다. */
  List<OrderItem> findAllByOrderId(Long orderId);
}
