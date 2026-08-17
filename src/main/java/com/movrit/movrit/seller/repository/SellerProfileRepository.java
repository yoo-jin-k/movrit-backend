package com.movrit.movrit.seller.repository;

import com.movrit.movrit.seller.entity.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    Optional<SellerProfile> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);
}
