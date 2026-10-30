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

/** 프리오더를 조회합니다. 주문 수량을 바꿀 때 쓰는 잠금 조회와 자동 진행(예약 실행)용 조회를 제공합니다. */
public interface PreorderRepository extends JpaRepository<Preorder, Long> {
  /** 프리오더를 쓰기 잠금으로 조회합니다. 동시에 들어온 주문이 주문 수량을 함께 바꾸지 못하게 합니다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Preorder p where p.id = :id")
  Optional<Preorder> findLockedById(Long id);

  /** 지정한 상태이면서 시작 시각이 지난 프리오더를 찾습니다. 판매 예약 프리오더를 자동으로 시작할 때 사용합니다. */
  List<Preorder> findAllByStatusAndStartAtLessThanEqual(PreorderStatus status, LocalDateTime now);

  /** 지정한 상태이면서 종료 시각이 지난 프리오더를 찾습니다. 판매 중 프리오더를 자동으로 마감할 때 사용합니다. */
  List<Preorder> findAllByStatusAndEndAtLessThanEqual(PreorderStatus status, LocalDateTime now);
}
