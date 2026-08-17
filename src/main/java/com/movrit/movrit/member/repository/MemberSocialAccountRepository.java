package com.movrit.movrit.member.repository;

import com.movrit.movrit.member.entity.AuthProvider;
import com.movrit.movrit.member.entity.MemberSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberSocialAccountRepository
        extends JpaRepository<MemberSocialAccount, Long> {

    Optional<MemberSocialAccount> findByProviderAndProviderId(
            AuthProvider provider,
            String providerId
    );
}
