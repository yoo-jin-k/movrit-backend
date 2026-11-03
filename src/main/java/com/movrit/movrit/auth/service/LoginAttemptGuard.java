package com.movrit.movrit.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 비밀번호를 계속 바꿔 넣어 맞히는 공격을 막기 위해 로그인 실패를 셉니다.
 *
 * <ul>
 *   <li>같은 이메일: 최근 15분 안에 5번 틀리면 로그인을 받지 않습니다. 15분이 지난 실패는 빠지므로 길어도 15분 뒤에는 풀립니다. 로그인에 성공하면 그 이메일의
 *       실패 기록을 지웁니다.
 *   <li>같은 접속 주소(IP): 최근 15분 안에 30번 틀리면 같은 방식으로 막습니다(여러 계정을 돌아가며 시도하는 경우).
 *   <li>막힌 동안에는 비밀번호가 맞아도 429로 거절합니다. 계정이 있는지 없는지는 알려 주지 않습니다.
 * </ul>
 *
 * <p>서버 한 대 기준으로 메모리에만 기록합니다. 서버를 여러 대로 늘리면 Redis 같은 공용 저장소로 옮겨야 합니다.
 */
@Component
public class LoginAttemptGuard {

  static final int EMAIL_LIMIT = 5;
  static final int ADDRESS_LIMIT = 30;
  static final Duration WINDOW = Duration.ofMinutes(15);
  static final String BLOCKED_MESSAGE = "로그인 시도가 너무 많습니다. 15분 뒤에 다시 시도해 주세요.";

  private final Clock clock;
  private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

  @Autowired
  public LoginAttemptGuard() {
    this(Clock.systemUTC());
  }

  LoginAttemptGuard(Clock clock) {
    this.clock = clock;
  }

  /** 로그인 처리 전에 부릅니다. 이메일이나 접속 주소가 막혀 있으면 429를 던집니다. */
  public void check(String email, String address) {
    Instant now = clock.instant();
    if (blocked(emailKey(email), EMAIL_LIMIT, now)
        || blocked(addressKey(address), ADDRESS_LIMIT, now)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, BLOCKED_MESSAGE);
    }
  }

  /** 이메일이 없거나 비밀번호가 틀렸을 때 부릅니다. */
  public void recordFailure(String email, String address) {
    Instant now = clock.instant();
    add(emailKey(email), now);
    add(addressKey(address), now);
  }

  /** 로그인에 성공하면 그 이메일의 실패 기록을 지웁니다(접속 주소 기록은 남깁니다). */
  public void recordSuccess(String email) {
    String key = emailKey(email);
    if (key != null) failures.remove(key);
  }

  private boolean blocked(String key, int limit, Instant now) {
    if (key == null) return false;
    Deque<Instant> times = failures.get(key);
    if (times == null) return false;
    synchronized (times) {
      prune(times, now);
      if (times.isEmpty()) {
        failures.remove(key, times);
        return false;
      }
      return times.size() >= limit;
    }
  }

  private void add(String key, Instant now) {
    if (key == null) return;
    Deque<Instant> times = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    synchronized (times) {
      prune(times, now);
      times.addLast(now);
    }
  }

  // 15분이 지난 실패 기록은 버립니다.
  private void prune(Deque<Instant> times, Instant now) {
    while (!times.isEmpty() && !times.peekFirst().plus(WINDOW).isAfter(now)) times.pollFirst();
  }

  private static String emailKey(String email) {
    return email == null || email.isBlank()
        ? null
        : "email:" + email.trim().toLowerCase(Locale.ROOT);
  }

  private static String addressKey(String address) {
    return address == null || address.isBlank() ? null : "addr:" + address;
  }
}
