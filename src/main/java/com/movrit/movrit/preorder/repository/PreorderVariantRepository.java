package com.movrit.movrit.preorder.repository;

import com.movrit.movrit.preorder.entity.PreorderVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PreorderVariantRepository extends JpaRepository<PreorderVariant, Long> {
    List<PreorderVariant> findAllByPreorderIdAndActiveTrue(Long preorderId);
    boolean existsByPreorderIdAndProductSkuId(Long preorderId, Long productSkuId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PreorderVariant> findWithLockById(Long id);
}
