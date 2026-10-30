package com.movrit.movrit.member.repository;

import com.movrit.movrit.member.entity.AuthProvider;
import com.movrit.movrit.member.entity.MemberSocialAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원에게 연결된 소셜 로그인(Google·카카오) 계정을 조회합니다. */
public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

  /** 소셜 서비스가 준 사용자 ID로 이미 연결된 MOVRIT 회원을 찾습니다. 소셜 로그인 시 기존 회원 여부를 판단합니다. */
  Optional<MemberSocialAccount> findByProviderAndProviderId(
      AuthProvider provider, String providerId);

  /** 회원에게 연결된 소셜 로그인 계정(Google·Kakao)을 모두 조회합니다. */
  List<MemberSocialAccount> findAllByMemberId(Long memberId);

  /** 회원에게 같은 종류의 소셜 계정이 이미 연결돼 있는지 확인합니다. */
  boolean existsByMemberIdAndProvider(Long memberId, AuthProvider provider);
}
