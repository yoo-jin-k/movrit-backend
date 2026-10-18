package com.movrit.movrit.preorder.repository;

import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PreorderRepository extends JpaRepository<Preorder, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Preorder p where p.id = :id")
  Optional<Preorder> findLockedById(Long id);

  List<Preorder> findAllByStatusAndStartAtLessThanEqual(PreorderStatus status, LocalDateTime now);

  List<Preorder> findAllByStatusAndEndAtLessThanEqual(PreorderStatus status, LocalDateTime now);
}
