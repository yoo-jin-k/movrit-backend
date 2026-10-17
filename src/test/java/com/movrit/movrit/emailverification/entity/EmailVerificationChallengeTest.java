package com.movrit.movrit.emailverification.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationChallengeTest {

    @Test
    void challengeBecomesUnusableAfterMaximumAttempts() {
        EmailVerificationChallenge challenge = challenge();

        for (int attempt = 0; attempt < 5; attempt += 1) {
            challenge.recordFailedAttempt(5);
        }

        assertThat(challenge.getAttemptCount()).isEqualTo(5);
        assertThat(challenge.isUsable(LocalDateTime.now())).isFalse();
    }

    @Test
    void consumedChallengeCannotBeUsedAgain() {
        EmailVerificationChallenge challenge = challenge();

        challenge.consume();

        assertThat(challenge.isUsable(LocalDateTime.now())).isFalse();
        assertThatThrownBy(challenge::consume)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("만료되었거나 이미 사용된 인증 요청입니다.");
    }

    private EmailVerificationChallenge challenge() {
        return EmailVerificationChallenge.builder()
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .attemptCount(0)
                .build();
    }
}
