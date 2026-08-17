package com.movrit.movrit.creator.repository;

import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.entity.CreatorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreatorRepository extends JpaRepository<Creator, Long> {
    Optional<Creator> findByMemberId(Long memberId);
    boolean existsByMemberId(Long memberId);
    List<Creator> findAllByStatusOrderByCreatedAtDesc(CreatorStatus status);
}
