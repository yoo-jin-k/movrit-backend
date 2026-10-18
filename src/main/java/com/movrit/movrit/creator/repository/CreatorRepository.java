package com.movrit.movrit.creator.repository;

import com.movrit.movrit.creator.entity.Creator;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorRepository extends JpaRepository<Creator, Long> {
  Optional<Creator> findByMemberId(Long memberId);

  boolean existsByMemberId(Long memberId);
}
