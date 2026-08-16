package com.movrit.movrit.order.repository;

import com.movrit.movrit.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
