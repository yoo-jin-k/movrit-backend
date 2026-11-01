package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * SNS 로그인 요청마다 한 번만 쓰는 state 값을 발급하고 확인합니다.
 *
 * <p>SNS 로그인에서 돌아왔을 때 state가 '이 회원이 이 서비스로 시작한 요청'인지 확인해, 다른 사람이 만든 인가 코드를 내 계정에 붙이는 공격(CSRF)을
 * 막습니다. state는 10분 동안만 유효하고 한 번 확인하면 사라집니다.
 *
 * <p>로컬 시연용 단일 서버 기준으로 메모리에 보관합니다. 서버를 다시 켜면 진행 중이던 연결은 처음부터 다시 시작해야 합니다.
 */
@Component
public class ChannelOAuthStateStore {
  static final Duration TTL = Duration.ofMinutes(10);

  private record Entry(Long memberId, ChannelProvider provider, Instant expiresAt) {}

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();
  private final SecureRandom random = new SecureRandom();
  private final Clock clock;

  public ChannelOAuthStateStore() {
    this(Clock.systemUTC());
  }

  ChannelOAuthStateStore(Clock clock) {
    this.clock = clock;
  }

  /** 새 state를 만들어 회원·서비스와 묶어 둡니다. 만료된 값은 이때 함께 정리합니다. */
  public String issue(Long memberId, ChannelProvider provider) {
    Instant now = clock.instant();
    entries.values().removeIf(entry -> entry.expiresAt().isBefore(now));
    byte[] bytes = new byte[24];
    random.nextBytes(bytes);
    String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    entries.put(state, new Entry(memberId, provider, now.plus(TTL)));
    return state;
  }

  /** state가 같은 회원·같은 서비스의 유효한 요청인지 확인하고 사용 처리합니다. 맞지 않으면 false입니다. */
  public boolean consume(String state, Long memberId, ChannelProvider provider) {
    Entry entry = state == null ? null : entries.remove(state);
    return entry != null
        && entry.memberId().equals(memberId)
        && entry.provider() == provider
        && entry.expiresAt().isAfter(clock.instant());
  }
}
