package com.movrit.movrit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.auth.dto.GoogleUserInfo;
import com.movrit.movrit.member.entity.AuthProvider;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import java.util.Optional;
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

  @Test
  void googleSignupWithAlreadyRegisteredEmailIsRejectedWithoutCreatingMember() {
    MemberSocialAccountRepository socialAccounts = mock(MemberSocialAccountRepository.class);
    MemberRepository members = mock(MemberRepository.class);
    GoogleAuthService service = new GoogleAuthService(socialAccounts, members);
    // 연결된 Google 계정은 없지만 같은 이메일로 가입한 회원은 있는 상황입니다.
    when(socialAccounts.findByProviderAndProviderId(AuthProvider.GOOGLE, "g-1"))
        .thenReturn(Optional.empty());
    when(members.existsByEmail("same@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> service.loginOrSignup(new GoogleUserInfo("g-1", "same@example.com", "같은 사람")))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> {
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
              assertThat(exception.getReason())
                  .isEqualTo(GoogleAuthService.EMAIL_ALREADY_REGISTERED);
            });
    verify(members, never()).save(any());
  }
}
