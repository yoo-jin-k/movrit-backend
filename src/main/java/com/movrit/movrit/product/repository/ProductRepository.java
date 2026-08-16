package com.movrit.movrit.product.repository;

import com.movrit.movrit.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
