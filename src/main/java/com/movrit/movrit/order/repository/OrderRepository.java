package com.movrit.movrit.order.repository;

import com.movrit.movrit.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByOrderNumber(String orderNumber);
    boolean existsByRequestKey(String requestKey);
    List<Order> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<Order> findAllByPreorderSellerId(Long sellerId);
}
