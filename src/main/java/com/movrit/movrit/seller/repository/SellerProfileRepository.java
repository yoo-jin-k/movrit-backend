package com.movrit.movrit.seller.repository;

import com.movrit.movrit.seller.entity.SellerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

  Optional<SellerProfile> findByMemberId(Long memberId);

  boolean existsByMemberId(Long memberId);
}
