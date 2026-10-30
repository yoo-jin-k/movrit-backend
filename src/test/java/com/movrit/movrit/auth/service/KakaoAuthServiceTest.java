package com.movrit.movrit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Kakao 인가 URL과 토큰 교환 요청이 설정된 콜백 주소·Client Secret 정책을 따르는지 확인합니다. */
class KakaoAuthServiceTest {

  private KakaoAuthService service(String secret) {
    KakaoAuthService service =
        new KakaoAuthService(
            mock(MemberSocialAccountRepository.class), mock(MemberRepository.class));
    ReflectionTestUtils.setField(service, "clientId", "rest-api-key");
    ReflectionTestUtils.setField(service, "clientSecret", secret);
    ReflectionTestUtils.setField(
        service, "redirectUri", "http://localhost:18080/api/auth/kakao/callback");
    return service;
  }

  @Test
  void loginUrlUsesConfiguredRedirectUri() {
    assertThat(service("").getLoginUrl(null))
        .contains("client_id=rest-api-key")
        .contains("redirect_uri=http://localhost:18080/api/auth/kakao/callback");
  }

  @Test
  void blankSecretIsNotSentToKakao() {
    assertThat(service("").tokenRequestBody("code-1")).doesNotContainKey("client_secret");
    assertThat(service(null).tokenRequestBody("code-1")).doesNotContainKey("client_secret");
  }

  @Test
  void configuredSecretAndSameRedirectUriAreSent() {
    var body = service("kakao-secret").tokenRequestBody("code-1");
    assertThat(body.getFirst("client_secret")).isEqualTo("kakao-secret");
    assertThat(body.getFirst("redirect_uri"))
        .isEqualTo("http://localhost:18080/api/auth/kakao/callback");
    assertThat(body.getFirst("code")).isEqualTo("code-1");
  }
}
