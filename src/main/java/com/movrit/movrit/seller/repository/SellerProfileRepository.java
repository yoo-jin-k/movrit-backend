package com.movrit.movrit.seller.repository;

import com.movrit.movrit.seller.entity.SellerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 업체(판매자) 프로필을 조회합니다. 회원 한 명당 하나의 업체 프로필만 가질 수 있습니다. */
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

  Optional<SellerProfile> findByMemberId(Long memberId);

  boolean existsByMemberId(Long memberId);
}
