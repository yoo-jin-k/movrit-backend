package com.movrit.movrit.channelconnect.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 네이버 아이디 이메일에서 블로그 아이디를 안전하게 뽑는지 확인합니다. */
class HttpChannelOAuthGatewayTest {

  @Test
  void naverBlogIdComesOnlyFromNaverEmail() {
    assertThat(HttpChannelOAuthGateway.naverBlogId("Movrit_Blog@naver.com"))
        .isEqualTo("movrit_blog");
    assertThat(HttpChannelOAuthGateway.naverBlogId("someone@gmail.com")).isNull();
    assertThat(HttpChannelOAuthGateway.naverBlogId("bad/../id@naver.com")).isNull();
    assertThat(HttpChannelOAuthGateway.naverBlogId(null)).isNull();
  }
}
