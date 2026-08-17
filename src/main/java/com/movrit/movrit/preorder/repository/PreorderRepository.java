package com.movrit.movrit.preorder.repository;

import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;import java.time.LocalDateTime;

public interface PreorderRepository extends JpaRepository<Preorder, Long> {
    List<Preorder> findAllByStatusAndStartAtLessThanEqual(PreorderStatus status, LocalDateTime now);
    List<Preorder> findAllByStatusAndEndAtLessThanEqual(PreorderStatus status, LocalDateTime now);
}
