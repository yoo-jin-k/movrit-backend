package com.movrit.movrit.collaboration.repository;

import com.movrit.movrit.collaboration.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기존 협업(프리오더·크리에이터)을 저장하고 조회합니다. */
public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {
  /** 같은 프리오더에 같은 크리에이터의 협업이 이미 있는지 확인합니다(중복 신청 방지). */
  boolean existsByPreorderIdAndCreatorId(Long p, Long c);

  /** 추천 링크의 코드로 협업을 찾습니다. */
  Optional<Collaboration> findByReferralCode(String code);

  /** 크리에이터 본인의 협업 목록을 최신순으로 조회합니다. */
  List<Collaboration> findAllByCreatorMemberIdOrderByIdDesc(Long id);

  /** 업체가 소유한 상품의 협업 목록을 최신순으로 조회합니다. */
  List<Collaboration> findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(Long id);
}
