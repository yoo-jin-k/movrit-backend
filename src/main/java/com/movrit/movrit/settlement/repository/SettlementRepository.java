package com.movrit.movrit.settlement.repository;

import com.movrit.movrit.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
}
