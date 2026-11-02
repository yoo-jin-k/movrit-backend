package com.movrit.movrit.passwordreset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.passwordreset.entity.PasswordResetToken;
import com.movrit.movrit.passwordreset.repository.PasswordResetTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** 비밀번호 찾기 안내가 가입 여부를 드러내지 않고, 재설정 링크가 한 번만·만료 전에만 동작하는지 확인합니다. */
class PasswordResetServiceTest {
  private final MemberRepository members = mock(MemberRepository.class);
  private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
  private final PasswordEncoder encoder = mock(PasswordEncoder.class);

  @SuppressWarnings("unchecked")
  private final ObjectProvider<JavaMailSender> mail = mock(ObjectProvider.class);

  private final PasswordResetService service =
      new PasswordResetService(members, tokens, mock(RefreshTokenRepository.class), encoder, mail);

  private Member member() {
    return Member.builder()
        .id(5L)
        .email("user@movrit.local")
        .passwordHash("hash")
        .status(MemberStatus.ACTIVE)
        .build();
  }

  @Test
  void unknownEmailGetsSameMessageWithoutToken() {
    ReflectionTestUtils.setField(service, "delivery", "local-preview");
    when(members.findByEmail("nobody@movrit.local")).thenReturn(Optional.empty());

    var result = service.requestReset(" Nobody@movrit.local ");

    assertThat(result.message()).isEqualTo(PasswordResetService.GENERIC_MESSAGE);
    assertThat(result.devResetToken()).isNull();
    verify(tokens, never()).save(any());
  }

  @Test
  void localPreviewReturnsTokenThatCanResetOnce() {
    ReflectionTestUtils.setField(service, "delivery", "local-preview");
    Member member = member();
    when(members.findByEmail("user@movrit.local")).thenReturn(Optional.of(member));

    var result = service.requestReset("user@movrit.local");
    assertThat(result.devResetToken()).isNotBlank();

    PasswordResetToken saved =
        PasswordResetToken.builder()
            .member(member)
            .tokenHash(PasswordResetService.hash(result.devResetToken()))
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .build();
    when(tokens.findByTokenHash(saved.getTokenHash())).thenReturn(Optional.of(saved));
    when(encoder.encode("newPass123")).thenReturn("new-hash");

    service.reset(result.devResetToken(), "newPass123");
    assertThat(member.getPasswordHash()).isEqualTo("new-hash");
    // 한 번 쓴 링크는 다시 쓸 수 없습니다.
    assertThat(service.validate(result.devResetToken()).valid()).isFalse();
    assertThatThrownBy(() -> service.reset(result.devResetToken(), "again123"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void expiredTokenIsInvalid() {
    PasswordResetToken expired =
        PasswordResetToken.builder()
            .member(member())
            .tokenHash(PasswordResetService.hash("old"))
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .build();
    when(tokens.findByTokenHash(PasswordResetService.hash("old"))).thenReturn(Optional.of(expired));

    assertThat(service.validate("old").valid()).isFalse();
  }
}
