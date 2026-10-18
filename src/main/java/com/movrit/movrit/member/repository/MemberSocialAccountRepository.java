package com.movrit.movrit.member.repository;

import com.movrit.movrit.member.entity.AuthProvider;
import com.movrit.movrit.member.entity.MemberSocialAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

  Optional<MemberSocialAccount> findByProviderAndProviderId(
      AuthProvider provider, String providerId);

  List<MemberSocialAccount> findAllByMemberId(Long memberId);

  boolean existsByMemberIdAndProvider(Long memberId, AuthProvider provider);
}
