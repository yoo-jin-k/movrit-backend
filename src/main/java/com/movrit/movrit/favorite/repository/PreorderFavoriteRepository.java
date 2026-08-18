package com.movrit.movrit.favorite.repository;

import com.movrit.movrit.favorite.entity.PreorderFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreorderFavoriteRepository extends JpaRepository<PreorderFavorite, Long> {
    boolean existsByMemberIdAndPreorderId(Long memberId, Long preorderId);
    Optional<PreorderFavorite> findByMemberIdAndPreorderId(Long memberId, Long preorderId);
    List<PreorderFavorite> findAllByMemberIdOrderByIdDesc(Long memberId);
    List<PreorderFavorite> findAllByPreorderId(Long preorderId);
}
