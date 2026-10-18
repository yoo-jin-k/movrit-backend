package com.movrit.movrit.auth.repository;

import com.movrit.movrit.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHash(String hash);

  Optional<RefreshToken> findByMemberId(Long id);
}
