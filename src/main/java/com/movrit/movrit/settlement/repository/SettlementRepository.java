package com.movrit.movrit.settlement.repository;

import com.movrit.movrit.settlement.entity.Settlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
  Optional<Settlement> findByPreorderId(Long preorderId);

  List<Settlement> findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(Long memberId);
}
