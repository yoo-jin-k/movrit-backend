package com.movrit.movrit.preorder.repository;

import com.movrit.movrit.preorder.entity.Preorder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreorderRepository extends JpaRepository<Preorder, Long> {
}
