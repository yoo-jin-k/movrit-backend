package com.movrit.movrit.settlement.repository;

import com.movrit.movrit.settlement.entity.Settlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 프리오더 정산을 조회합니다. */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
  /** 프리오더의 정산을 찾습니다. 프리오더 한 건에는 정산이 하나만 있습니다. */
  Optional<Settlement> findByPreorderId(Long preorderId);

  /** 업체가 소유한 브랜드 프리오더의 정산을 최근 순으로 조회합니다. */
  List<Settlement> findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(Long memberId);
}
