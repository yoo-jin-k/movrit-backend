package com.movrit.movrit.emailverification.service;

import com.movrit.movrit.emailverification.entity.EmailVerificationChallenge;
import com.movrit.movrit.emailverification.repository.EmailVerificationChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 이메일 인증 코드 확인 실패 횟수 기록과 코드 무효화를 별도 트랜잭션으로 처리합니다. 확인 요청이 오류로 끝나도 실패 기록은 남게 하려는 목적입니다. */
@Service
@RequiredArgsConstructor
public class EmailVerificationAttemptService {
  private final EmailVerificationChallengeRepository repository;

  /**
   * 인증 코드 확인 실패를 1회 기록합니다. 최대 시도 횟수에 도달하면 해당 코드는 더 이상 쓸 수 없게 됩니다. 바깥 요청이 실패해도 기록이 사라지지 않도록 새 트랜잭션에서
   * 저장합니다.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(Long challengeId, Long memberId, int maxAttempts) {
    getOwned(challengeId, memberId).recordFailedAttempt(maxAttempts);
  }

  /** 본인의 인증 코드를 즉시 무효화합니다. 만료된 코드를 확인하려 할 때 등에 사용합니다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void invalidate(Long challengeId, Long memberId) {
    getOwned(challengeId, memberId).invalidate();
  }

  private EmailVerificationChallenge getOwned(Long challengeId, Long memberId) {
    return repository
        .findOwnedForUpdate(challengeId, memberId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."));
  }
}
