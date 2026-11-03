package com.movrit.movrit.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** 로그인 실패 제한(이메일 5회·접속 주소 30회, 15분)을 확인합니다. */
class LoginAttemptGuardTest {

  private Instant now = Instant.parse("2026-11-04T00:00:00Z");

  private LoginAttemptGuard guard() {
    return new LoginAttemptGuard(
        new Clock() {
          @Override
          public Instant instant() {
            return now;
          }

          @Override
          public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return this;
          }
        });
  }

  @Test
  void emailIsBlockedAfterFiveFailuresAndReleasedAfterWindow() {
    LoginAttemptGuard guard = guard();
    for (int i = 0; i < 4; i++) guard.recordFailure("User@Movrit.local", "1.1.1.1");
    assertThatCode(() -> guard.check("user@movrit.local", "2.2.2.2")).doesNotThrowAnyException();

    guard.recordFailure("user@movrit.local", "1.1.1.1");
    // 대소문자·접속 주소가 달라도 같은 이메일이면 막힙니다.
    assertThatThrownBy(() -> guard.check("USER@movrit.local", "3.3.3.3"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("15분");

    now = now.plus(LoginAttemptGuard.WINDOW).plus(Duration.ofSeconds(1));
    assertThatCode(() -> guard.check("user@movrit.local", "1.1.1.1")).doesNotThrowAnyException();
  }

  @Test
  void successClearsEmailFailures() {
    LoginAttemptGuard guard = guard();
    for (int i = 0; i < 4; i++) guard.recordFailure("a@movrit.local", "1.1.1.1");
    guard.recordSuccess("a@movrit.local");
    guard.recordFailure("a@movrit.local", "1.1.1.1");
    assertThatCode(() -> guard.check("a@movrit.local", "1.1.1.1")).doesNotThrowAnyException();
  }

  @Test
  void addressIsBlockedAfterThirtyFailuresAcrossAccounts() {
    LoginAttemptGuard guard = guard();
    for (int i = 0; i < 30; i++) guard.recordFailure("user" + i + "@movrit.local", "9.9.9.9");
    assertThatThrownBy(() -> guard.check("new@movrit.local", "9.9.9.9"))
        .isInstanceOf(ResponseStatusException.class);
    assertThatCode(() -> guard.check("new@movrit.local", "8.8.8.8")).doesNotThrowAnyException();
  }
}
