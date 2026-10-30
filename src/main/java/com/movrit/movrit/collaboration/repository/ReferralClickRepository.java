package com.movrit.movrit.collaboration.repository;

import com.movrit.movrit.collaboration.entity.ReferralClick;
import org.springframework.data.jpa.repository.JpaRepository;

/** 추천 링크 클릭 기록을 저장하고 협업별 클릭 수를 셉니다. */
public interface ReferralClickRepository extends JpaRepository<ReferralClick, Long> {
  /** 협업 추천 링크의 클릭 수를 셉니다. */
  long countByCollaborationId(Long id);
}
