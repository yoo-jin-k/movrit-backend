package com.movrit.movrit.emailverification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.emailverification.dto.EmailVerificationDtos;
import com.movrit.movrit.emailverification.entity.EmailVerificationChallenge;
import com.movrit.movrit.emailverification.repository.EmailVerificationChallengeRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** 로컬 시연 모드에서만 인증 코드가 응답에 담기고, 실제 발송 방식에서는 담기지 않는지 확인합니다. */
class EmailVerificationLocalPreviewTest {

  private EmailVerificationDtos.RequestResponse request(VerificationCodeDelivery delivery) {
    EmailVerificationChallengeRepository repository =
        mock(EmailVerificationChallengeRepository.class);
    MemberService members = mock(MemberService.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    when(members.getMember(7L))
        .thenReturn(Member.builder().id(7L).email("user@example.com").build());
    when(repository.findFirstByMemberIdOrderByIdDesc(7L)).thenReturn(Optional.empty());
    when(repository.countByMemberIdAndCreatedAtAfter(anyLong(), any())).thenReturn(0L);
    when(repository.findAllByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(7L))
        .thenReturn(List.of());
    when(encoder.encode(anyString())).thenReturn("hashed");
    when(repository.save(any(EmailVerificationChallenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    EmailVerificationService service =
        new EmailVerificationService(
            repository, members, encoder, delivery, mock(EmailVerificationAttemptService.class));
    ReflectionTestUtils.setField(service, "expiresMinutes", 10);
    ReflectionTestUtils.setField(service, "resendSeconds", 60);
    ReflectionTestUtils.setField(service, "hourlyLimit", 5);
    return service.request(7L);
  }

  @Test
  void localPreviewDeliveryReturnsSixDigitCodeForScreen() {
    VerificationCodeDelivery delivery = new LocalPreviewVerificationCodeDelivery();
    EmailVerificationDtos.RequestResponse response = request(delivery);
    assertThat(response.localPreviewCode()).matches("^[0-9]{6}$");
  }

  @Test
  void realDeliveryNeverExposesCode() {
    // 실제 발송 방식(기본 구현)은 코드를 응답에 담지 않습니다.
    VerificationCodeDelivery delivery = mock(VerificationCodeDelivery.class);
    EmailVerificationDtos.RequestResponse response = request(delivery);
    assertThat(response.localPreviewCode()).isNull();
    verify(delivery).send(eq("user@example.com"), anyString(), anyInt());
  }
}
