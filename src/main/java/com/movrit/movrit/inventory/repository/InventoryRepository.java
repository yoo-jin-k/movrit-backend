package com.movrit.movrit.inventory.repository;

import com.movrit.movrit.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductSkuId(Long productSkuId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productSku.id = :productSkuId")
    Optional<Inventory> findByProductSkuIdForUpdate(@Param("productSkuId") Long productSkuId);
}
