package com.movrit.movrit.seller.repository;

import com.movrit.movrit.seller.entity.SellerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 업체(판매자) 프로필을 조회합니다. 회원 한 명당 하나의 업체 프로필만 가질 수 있습니다. */
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

  /** 회원의 업체(판매자) 정보를 찾습니다. */
  Optional<SellerProfile> findByMemberId(Long memberId);

  /** 회원이 업체 신청을 이미 했는지 확인합니다. */
  boolean existsByMemberId(Long memberId);
}
