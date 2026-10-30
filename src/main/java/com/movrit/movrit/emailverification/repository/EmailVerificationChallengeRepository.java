package com.movrit.movrit.emailverification.repository;

import com.movrit.movrit.emailverification.entity.EmailVerificationChallenge;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 이메일 인증 코드 발급 기록을 조회합니다. */
public interface EmailVerificationChallengeRepository
    extends JpaRepository<EmailVerificationChallenge, Long> {
  /** 아직 사용·무효화되지 않은 인증 코드를 모두 찾습니다. 새 코드를 보내기 전에 이전 코드를 무효화할 때 씁니다. */
  List<EmailVerificationChallenge> findAllByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(
      Long memberId);

  /** 일정 시간 안에 보낸 인증 코드 수를 셉니다. 짧은 시간에 너무 자주 보내는 것을 막는 데 씁니다. */
  long countByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime since);

  Optional<EmailVerificationChallenge> findFirstByMemberIdOrderByIdDesc(Long memberId);

  /** 본인 인증 코드를 쓰기 잠금으로 조회합니다. 같은 코드를 동시에 여러 번 확인하는 것을 막습니다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select challenge from EmailVerificationChallenge challenge where challenge.id = :id and challenge.member.id = :memberId")
  Optional<EmailVerificationChallenge> findOwnedForUpdate(
      @Param("id") Long id, @Param("memberId") Long memberId);
}
