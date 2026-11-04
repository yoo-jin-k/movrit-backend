package com.movrit.movrit.phoneverification.service;

import com.movrit.movrit.phoneverification.entity.PhoneVerificationChallenge;
import com.movrit.movrit.phoneverification.repository.PhoneVerificationChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 인증번호 틀린 입력 기록을 별도 트랜잭션으로 저장합니다. 확인 요청이 오류로 끝나도 실패 횟수는 남게 하려는 목적입니다. */
@Service
@RequiredArgsConstructor
public class PhoneVerificationAttemptService {
  private final PhoneVerificationChallengeRepository repository;

  /** 틀린 입력을 1회 기록합니다. 최대 횟수에 닿으면 그 인증번호는 더 쓸 수 없습니다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(Long challengeId, Long memberId, int maxAttempts) {
    repository
        .findOwnedForUpdate(challengeId, memberId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."))
        .recordFailedAttempt(maxAttempts);
  }

  /** 본인 인증번호를 바로 무효화합니다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void invalidate(Long challengeId, Long memberId) {
    repository
        .findOwnedForUpdate(challengeId, memberId)
        .ifPresent(PhoneVerificationChallenge::invalidate);
  }
}
