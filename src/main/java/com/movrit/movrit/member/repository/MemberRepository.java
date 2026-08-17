package com.movrit.movrit.member.repository;

import com.movrit.movrit.member.entity.AuthProvider;
import java.util.Optional;
import com.movrit.movrit.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);

    Optional<Member> findByAuthProviderAndProviderId(
            AuthProvider authProvider,
            String providerId
    );
}
