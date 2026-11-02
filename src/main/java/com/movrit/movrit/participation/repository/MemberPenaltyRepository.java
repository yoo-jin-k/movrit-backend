package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.MemberPenalty;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 패널티 저장소입니다. */
public interface MemberPenaltyRepository extends JpaRepository<MemberPenalty, Long> {
  List<MemberPenalty> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

  List<MemberPenalty> findTop200ByOrderByCreatedAtDesc();

  boolean existsByApplicationId(Long applicationId);
}
