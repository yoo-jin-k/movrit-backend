package com.movrit.movrit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class GoogleAuthServiceTest {

  @Test
  void integrationDummyCredentialsDoNotOpenGoogleAuthorization() {
    GoogleAuthService service =
        new GoogleAuthService(
            mock(MemberSocialAccountRepository.class), mock(MemberRepository.class));
    ReflectionTestUtils.setField(service, "clientId", "integration-test");
    ReflectionTestUtils.setField(service, "clientSecret", "integration-test");
    ReflectionTestUtils.setField(
        service, "redirectUri", "http://localhost:18080/api/auth/google/callback");

    assertThatThrownBy(service::getLoginUrl)
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> {
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
              assertThat(exception.getReason()).isEqualTo("구글 로그인 설정이 아직 구성되지 않았습니다.");
            });
  }
}
