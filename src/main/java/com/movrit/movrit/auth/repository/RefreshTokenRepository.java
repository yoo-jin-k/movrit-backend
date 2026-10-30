package com.movrit.movrit.auth.repository;

import com.movrit.movrit.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 로그인 유지용 리프레시 토큰을 저장합니다. 토큰 원문이 아닌 해시값으로만 조회합니다. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  /** 갱신 토큰의 해시값으로 저장된 토큰을 찾습니다. 원래 토큰 값은 저장하지 않습니다. */
  Optional<RefreshToken> findByTokenHash(String hash);

  /** 회원의 갱신 토큰을 찾습니다. 회원당 하나만 유지합니다. */
  Optional<RefreshToken> findByMemberId(Long id);
}
