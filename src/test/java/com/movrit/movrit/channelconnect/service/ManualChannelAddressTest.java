package com.movrit.movrit.channelconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import org.junit.jupiter.api.Test;

/** 직접 등록 주소를 서비스별로 검사하고 표준 주소로 바꾸는지 확인합니다. */
class ManualChannelAddressTest {

  @Test
  void normalizesEachProviderAddress() {
    assertThat(
            ManualChannelAddress.parse(
                ChannelProvider.INSTAGRAM, "https://instagram.com/Daily.Look?igsh=x"))
        .isEqualTo(
            new ManualChannelAddress.Parsed(
                "manual:daily.look", "@daily.look", "https://www.instagram.com/daily.look"));
    assertThat(
            ManualChannelAddress.parse(ChannelProvider.YOUTUBE, "youtube.com/@MovritTV")
                .profileUrl())
        .isEqualTo("https://www.youtube.com/@MovritTV");
    assertThat(
            ManualChannelAddress.parse(
                    ChannelProvider.NAVER_BLOG, "https://m.blog.naver.com/movrit_blog")
                .profileUrl())
        .isEqualTo("https://blog.naver.com/movrit_blog");
  }

  @Test
  void rejectsOtherSitesAndScripts() {
    assertThatThrownBy(
            () -> ManualChannelAddress.parse(ChannelProvider.INSTAGRAM, "javascript:alert(1)"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                ManualChannelAddress.parse(
                    ChannelProvider.NAVER_BLOG, "https://blog.naver.com.evil.com/a"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
