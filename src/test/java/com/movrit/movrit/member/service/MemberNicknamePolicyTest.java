package com.movrit.movrit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MemberNicknamePolicyTest {

  @Test
  void normalizeTrimsAndCollapsesWhitespace() {
    assertThat(MemberNicknamePolicy.normalize("  모브릿   사용자  ")).isEqualTo("모브릿 사용자");
  }

  @Test
  void normalizeRejectsInvalidLengthAndCharacters() {
    assertThatThrownBy(() -> MemberNicknamePolicy.normalize("가"))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> MemberNicknamePolicy.normalize("123456789012345678901"))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> MemberNicknamePolicy.normalize("모브릿!"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void availableSocialFallbackRetriesCollision() {
    AtomicInteger checks = new AtomicInteger();

    String nickname =
        MemberNicknamePolicy.availableSocialFallback(value -> checks.getAndIncrement() == 0);

    assertThat(checks).hasValue(2);
    assertThat(nickname).matches("social_[a-f0-9]{8}");
    assertThat(MemberNicknamePolicy.normalize(nickname)).isEqualTo(nickname);
  }
}
