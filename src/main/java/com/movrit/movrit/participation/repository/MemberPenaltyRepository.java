package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.MemberPenalty;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 패널티 저장소입니다. */
public interface MemberPenaltyRepository extends JpaRepository<MemberPenalty, Long> {
  List<MemberPenalty> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

  List<MemberPenalty> findTop200ByOrderByCreatedAtDesc();

  boolean existsByApplicationId(Long applicationId);

  /** 같은 신청에 이미 기록한 종류(미제출·노쇼 등)가 있는지 확인합니다. */
  boolean existsByApplicationIdAndPenaltyTypeIn(
      Long applicationId, Collection<String> penaltyTypes);
}
