package com.movrit.movrit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.auth.entity.RefreshToken;
import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.member.dto.MemberPasswordCapabilityResponse;
import com.movrit.movrit.member.dto.MemberPasswordChangeRequest;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class MemberPasswordServiceTest {

  @Test
  void socialOnlyAccountCannotChangePassword() {
    MemberRepository members = mock(MemberRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    Member member = Member.builder().id(1L).passwordHash(null).build();
    when(members.findById(1L)).thenReturn(Optional.of(member));
    when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
    MemberService service = new MemberService(members, encoder, refreshTokens);

    MemberPasswordCapabilityResponse capability = service.passwordCapability(1L);

    assertThat(capability.hasPassword()).isFalse();
    assertThat(capability.canChangePassword()).isFalse();
    assertThat(capability.unavailableReason()).isEqualTo("SOCIAL_ONLY_ACCOUNT");
    assertThatThrownBy(
            () ->
                service.changePassword(
                    1L, new MemberPasswordChangeRequest("Current1", "NextPassword1")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("PASSWORD_NOT_CONFIGURED");
    verify(encoder, never())
        .matches(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void rejectsUtf8ByteOverflowBeforeCallingBcrypt() {
    MemberRepository members = mock(MemberRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    Member member = Member.builder().id(1L).passwordHash("hash").build();
    when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
    String tooLong = "한".repeat(25);

    assertThatThrownBy(
            () ->
                new MemberService(members, encoder, refreshTokens)
                    .changePassword(1L, new MemberPasswordChangeRequest(tooLong, "NextPassword1")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("CURRENT_PASSWORD_TOO_LONG_IN_BYTES");

    verify(encoder, never())
        .matches(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void successfulChangeRevokesRefreshSession() {
    MemberRepository members = mock(MemberRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    Member member = Member.builder().id(1L).passwordHash("old-hash").build();
    RefreshToken refreshToken = mock(RefreshToken.class);
    when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
    when(encoder.matches("Current1", "old-hash")).thenReturn(true);
    when(encoder.matches("NextPassword1", "old-hash")).thenReturn(false);
    when(encoder.encode("NextPassword1")).thenReturn("new-hash");
    when(refreshTokens.findByMemberId(1L)).thenReturn(Optional.of(refreshToken));

    new MemberService(members, encoder, refreshTokens)
        .changePassword(1L, new MemberPasswordChangeRequest("Current1", "NextPassword1"));

    assertThat(member.getPasswordHash()).isEqualTo("new-hash");
    verify(refreshToken).revoke();
  }
}
