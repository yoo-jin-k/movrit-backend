package com.movrit.movrit.preorder.repository;

import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;import java.time.LocalDateTime;

public interface PreorderRepository extends JpaRepository<Preorder, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Preorder p where p.id = :id")
    Optional<Preorder> findLockedById(Long id);
    List<Preorder> findAllByStatusAndStartAtLessThanEqual(PreorderStatus status, LocalDateTime now);
    List<Preorder> findAllByStatusAndEndAtLessThanEqual(PreorderStatus status, LocalDateTime now);
}
