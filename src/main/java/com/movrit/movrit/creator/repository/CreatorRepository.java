package com.movrit.movrit.creator.repository;

import com.movrit.movrit.creator.entity.Creator;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 캠페인 참여자 활동 프로필(Creator)을 조회합니다. 회원 한 명당 하나의 프로필을 가집니다. */
public interface CreatorRepository extends JpaRepository<Creator, Long> {
  Optional<Creator> findByMemberId(Long memberId);

  boolean existsByMemberId(Long memberId);
}
