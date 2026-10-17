package com.movrit.movrit.emailverification.service;

import com.movrit.movrit.emailverification.entity.EmailVerificationChallenge;
import com.movrit.movrit.emailverification.repository.EmailVerificationChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmailVerificationAttemptService {
    private final EmailVerificationChallengeRepository repository;

    /** 확인 실패 기록은 응답 예외와 무관하게 별도 트랜잭션으로 확정합니다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long challengeId, Long memberId, int maxAttempts) {
        getOwned(challengeId, memberId).recordFailedAttempt(maxAttempts);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidate(Long challengeId, Long memberId) {
        getOwned(challengeId, memberId).invalidate();
    }

    private EmailVerificationChallenge getOwned(Long challengeId, Long memberId) {
        return repository.findOwnedForUpdate(challengeId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."));
    }
}
