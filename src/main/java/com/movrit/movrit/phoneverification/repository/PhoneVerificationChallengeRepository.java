package com.movrit.movrit.phoneverification.repository;

import com.movrit.movrit.phoneverification.entity.PhoneVerificationChallenge;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 휴대폰 인증번호 발급 기록을 조회합니다. */
public interface PhoneVerificationChallengeRepository
    extends JpaRepository<PhoneVerificationChallenge, Long> {

  /** 아직 사용·무효화되지 않은 인증번호입니다. 새 번호를 보내기 전에 이전 번호를 무효화할 때 씁니다. */
  List<PhoneVerificationChallenge> findAllByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(
      Long memberId);

  /** 회원이 일정 시간 안에 받은 인증번호 수입니다(시간당 횟수 제한). */
  long countByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime since);

  /** 같은 번호로 일정 시간 안에 보낸 인증번호 수입니다(번호별 하루 횟수 제한, 회원과 무관). */
  long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime since);

  /** 회원이 가장 최근에 받은 인증번호입니다(재발송 간격 확인). */
  Optional<PhoneVerificationChallenge> findFirstByMemberIdOrderByIdDesc(Long memberId);

  /** 본인 인증번호를 쓰기 잠금으로 조회합니다. 같은 번호를 동시에 여러 번 확인하는 것을 막습니다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select challenge from PhoneVerificationChallenge challenge"
          + " where challenge.id = :id and challenge.member.id = :memberId")
  Optional<PhoneVerificationChallenge> findOwnedForUpdate(
      @Param("id") Long id, @Param("memberId") Long memberId);
}
