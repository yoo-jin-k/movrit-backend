package com.movrit.movrit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.auth.dto.LoginRequest;
import com.movrit.movrit.auth.jwt.JwtTokenProvider;
import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceLoginTest {

  @Test
  void unknownEmailAndWrongPasswordReturnSameUnauthorizedResponse() throws Exception {
    MemberRepository members = mock(MemberRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    AuthService service = service(members, encoder);
    LoginRequest unknownEmail = request("missing@movrit.local", "WrongPassword1");
    LoginRequest wrongPassword = request("member@movrit.local", "WrongPassword1");
    Member member =
        Member.builder().passwordHash("stored-hash").status(MemberStatus.ACTIVE).build();
    when(members.findByEmail("missing@movrit.local")).thenReturn(Optional.empty());
    when(members.findByEmail("member@movrit.local")).thenReturn(Optional.of(member));
    when(encoder.matches("WrongPassword1", "stored-hash")).thenReturn(false);

    ResponseStatusException missing = unauthorized(() -> service.login(unknownEmail));
    ResponseStatusException mismatch = unauthorized(() -> service.login(wrongPassword));

    assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(mismatch.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(missing.getReason()).isEqualTo("이메일 또는 비밀번호가 올바르지 않습니다.");
    assertThat(mismatch.getReason()).isEqualTo(missing.getReason());
  }

  @Test
  void socialOnlyAndOversizedPasswordsFailBeforeHashComparison() throws Exception {
    MemberRepository members = mock(MemberRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    AuthService service = service(members, encoder);
    Member socialOnly = Member.builder().passwordHash(null).status(MemberStatus.ACTIVE).build();
    Member local = Member.builder().passwordHash("stored-hash").status(MemberStatus.ACTIVE).build();
    when(members.findByEmail("social@movrit.local")).thenReturn(Optional.of(socialOnly));
    when(members.findByEmail("member@movrit.local")).thenReturn(Optional.of(local));

    unauthorized(() -> service.login(request("social@movrit.local", "Password1")));
    unauthorized(() -> service.login(request("member@movrit.local", "한".repeat(25))));

    verify(encoder, never())
        .matches(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  private static AuthService service(MemberRepository members, PasswordEncoder encoder) {
    return new AuthService(
        members, encoder, mock(JwtTokenProvider.class), mock(RefreshTokenRepository.class));
  }

  private static LoginRequest request(String email, String password) throws Exception {
    LoginRequest request = new LoginRequest();
    set(request, "email", email);
    set(request, "password", password);
    return request;
  }

  private static void set(LoginRequest request, String name, String value) throws Exception {
    Field field = LoginRequest.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(request, value);
  }

  private static ResponseStatusException unauthorized(ThrowingCall call) {
    return assertThrows(ResponseStatusException.class, call::run);
  }

  @FunctionalInterface
  private interface ThrowingCall {
    void run() throws Exception;
  }
}
