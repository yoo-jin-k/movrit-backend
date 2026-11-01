package com.movrit.movrit.channelconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** SNS 로그인 state가 같은 회원·같은 서비스에서 한 번만, 제한 시간 안에만 통과하는지 확인합니다. */
class ChannelOAuthStateStoreTest {

  @Test
  void stateIsSingleUseAndBoundToMemberAndProvider() {
    ChannelOAuthStateStore store = new ChannelOAuthStateStore();
    String state = store.issue(1L, ChannelProvider.YOUTUBE);

    // 다른 회원·다른 서비스로는 통과하지 않고, 확인을 시도한 state는 사라집니다.
    assertThat(store.consume(state, 2L, ChannelProvider.YOUTUBE)).isFalse();
    String second = store.issue(1L, ChannelProvider.YOUTUBE);
    assertThat(store.consume(second, 1L, ChannelProvider.INSTAGRAM)).isFalse();

    String third = store.issue(1L, ChannelProvider.YOUTUBE);
    assertThat(store.consume(third, 1L, ChannelProvider.YOUTUBE)).isTrue();
    assertThat(store.consume(third, 1L, ChannelProvider.YOUTUBE)).isFalse();
    assertThat(store.consume(null, 1L, ChannelProvider.YOUTUBE)).isFalse();
  }

  @Test
  void expiredStateIsRejected() {
    MutableClock clock = new MutableClock(Instant.parse("2026-11-02T00:00:00Z"));
    ChannelOAuthStateStore store = new ChannelOAuthStateStore(clock);
    String state = store.issue(1L, ChannelProvider.NAVER_BLOG);

    clock.now = clock.now.plus(ChannelOAuthStateStore.TTL).plusSeconds(1);

    assertThat(store.consume(state, 1L, ChannelProvider.NAVER_BLOG)).isFalse();
  }

  /** 테스트에서 시간을 앞으로 돌리기 위한 시계입니다. */
  private static final class MutableClock extends Clock {
    private Instant now;

    private MutableClock(Instant now) {
      this.now = now;
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
