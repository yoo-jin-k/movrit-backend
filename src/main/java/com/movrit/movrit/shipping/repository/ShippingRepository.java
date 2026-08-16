package com.movrit.movrit.shipping.repository;

import com.movrit.movrit.shipping.entity.Shipping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingRepository extends JpaRepository<Shipping, Long> {
}
