package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.MemberCancelCount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 참여자 취소 횟수 저장소입니다. */
public interface MemberCancelCountRepository extends JpaRepository<MemberCancelCount, Long> {
  List<MemberCancelCount> findAllByMemberIdOrderByCreatedAtAsc(Long memberId);

  boolean existsByApplicationId(Long applicationId);
}
