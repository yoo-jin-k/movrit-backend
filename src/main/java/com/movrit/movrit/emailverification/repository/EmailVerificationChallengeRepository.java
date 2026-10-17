package com.movrit.movrit.emailverification.repository;

import com.movrit.movrit.emailverification.entity.EmailVerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationChallengeRepository extends JpaRepository<EmailVerificationChallenge, Long> {
    List<EmailVerificationChallenge> findAllByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(Long memberId);

    long countByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime since);

    Optional<EmailVerificationChallenge> findFirstByMemberIdOrderByIdDesc(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from EmailVerificationChallenge challenge where challenge.id = :id and challenge.member.id = :memberId")
    Optional<EmailVerificationChallenge> findOwnedForUpdate(@Param("id") Long id, @Param("memberId") Long memberId);
}
