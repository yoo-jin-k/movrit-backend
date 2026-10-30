package com.movrit.movrit.auth.repository;

import com.movrit.movrit.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 로그인 유지용 리프레시 토큰을 저장합니다. 토큰 원문이 아닌 해시값으로만 조회합니다. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHash(String hash);

  Optional<RefreshToken> findByMemberId(Long id);
}
