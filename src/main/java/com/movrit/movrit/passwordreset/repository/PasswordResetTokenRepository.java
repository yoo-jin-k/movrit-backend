package com.movrit.movrit.passwordreset.repository;

import com.movrit.movrit.passwordreset.entity.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 비밀번호 재설정 토큰을 조회합니다. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  @EntityGraph(attributePaths = "member")
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  /** 아직 쓰지 않은 토큰들입니다(새 링크를 보내면 모두 막습니다). */
  List<PasswordResetToken> findAllByMemberIdAndUsedAtIsNull(Long memberId);

  /** 짧은 시간에 너무 많이 요청하지 못하도록 최근 발급 수를 셉니다. */
  long countByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime after);
}
