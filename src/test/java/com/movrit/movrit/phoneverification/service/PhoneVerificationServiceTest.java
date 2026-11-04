package com.movrit.movrit.phoneverification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.phoneverification.dto.PhoneVerificationDtos;
import com.movrit.movrit.phoneverification.entity.PhoneVerificationChallenge;
import com.movrit.movrit.phoneverification.repository.PhoneVerificationChallengeRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** 휴대폰 인증번호 발송·확인 규칙(형식, 횟수 제한, 다른 회원 번호, 시연 모드 표시, 연락처 변경 시 재인증)을 확인합니다. */
class PhoneVerificationServiceTest {
  private final PhoneVerificationChallengeRepository repository =
      mock(PhoneVerificationChallengeRepository.class);
  private final MemberService members = mock(MemberService.class);
  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final PasswordEncoder encoder = mock(PasswordEncoder.class);
  private final PhoneVerificationAttemptService attempts =
      mock(PhoneVerificationAttemptService.class);
  private final Member member = Member.builder().id(7L).name("참여자").build();

  private PhoneVerificationService service(PhoneCodeSender sender) {
    PhoneVerificationService service =
        new PhoneVerificationService(
            repository, members, memberRepository, encoder, sender, attempts);
    ReflectionTestUtils.setField(service, "expiresMinutes", 5);
    ReflectionTestUtils.setField(service, "resendSeconds", 60);
    ReflectionTestUtils.setField(service, "hourlyLimit", 5);
    ReflectionTestUtils.setField(service, "numberDailyLimit", 10);
    ReflectionTestUtils.setField(service, "maxAttempts", 5);
    when(members.getMember(7L)).thenReturn(member);
    when(repository.findFirstByMemberIdOrderByIdDesc(7L)).thenReturn(Optional.empty());
    when(repository.findAllByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(7L))
        .thenReturn(List.of());
    when(encoder.encode(anyString())).thenReturn("hashed");
    when(repository.save(any(PhoneVerificationChallenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    return service;
  }

  private PhoneVerificationChallenge challenge(Long id) {
    return PhoneVerificationChallenge.builder()
        .id(id)
        .member(member)
        .phone("01012345678")
        .codeHash("hashed")
        .expiresAt(LocalDateTime.now().plusMinutes(5))
        .attemptCount(0)
        .build();
  }

  private static HttpStatus status(Throwable exception) {
    return HttpStatus.valueOf(((ResponseStatusException) exception).getStatusCode().value());
  }

  @Test
  void phoneNumberIsNormalizedAndMasked() {
    assertThat(PhoneNumbers.normalize("010-1234-5678")).isEqualTo("01012345678");
    assertThat(PhoneNumbers.normalize(" 011 123 4567")).isEqualTo("0111234567");
    assertThat(PhoneNumbers.mask("01012345678")).isEqualTo("010-****-5678");
    assertThatThrownBy(() -> PhoneNumbers.normalize("02-123-4567"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PhoneNumbers.normalize("010-12-34"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void localPreviewShowsCodeAndRealSenderNever() {
    PhoneVerificationDtos.SendResponse preview =
        service(new LocalPreviewPhoneCodeSender())
            .send(7L, new PhoneVerificationDtos.SendRequest("010-1234-5678"));
    assertThat(preview.localPreviewCode()).matches("^[0-9]{6}$");
    assertThat(preview.maskedPhone()).isEqualTo("010-****-5678");

    PhoneCodeSender real = mock(PhoneCodeSender.class);
    PhoneVerificationDtos.SendResponse sent =
        service(real).send(7L, new PhoneVerificationDtos.SendRequest("01012345678"));
    assertThat(sent.localPreviewCode()).isNull();
    verify(real).send(eq("01012345678"), anyString(), anyInt());
  }

  @Test
  void limitsAndTakenNumbersAreRejected() {
    PhoneVerificationService service = service(new LocalPreviewPhoneCodeSender());
    PhoneVerificationDtos.SendRequest request =
        new PhoneVerificationDtos.SendRequest("010-1234-5678");

    // 다른 회원이 이미 인증한 번호
    when(memberRepository.existsByPhoneAndPhoneVerifiedAtIsNotNullAndIdNot("01012345678", 7L))
        .thenReturn(true);
    assertThatThrownBy(() -> service.send(7L, request))
        .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.CONFLICT));
    when(memberRepository.existsByPhoneAndPhoneVerifiedAtIsNotNullAndIdNot("01012345678", 7L))
        .thenReturn(false);

    // 같은 번호로 하루 10번을 넘으면 회원과 상관없이 막습니다.
    when(repository.countByPhoneAndCreatedAtAfter(eq("01012345678"), any())).thenReturn(10L);
    assertThatThrownBy(() -> service.send(7L, request))
        .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    when(repository.countByPhoneAndCreatedAtAfter(eq("01012345678"), any())).thenReturn(0L);

    // 1분 안에 다시 요청하면 막습니다.
    PhoneVerificationChallenge recent = challenge(1L);
    ReflectionTestUtils.setField(recent, "createdAt", LocalDateTime.now());
    when(repository.findFirstByMemberIdOrderByIdDesc(7L)).thenReturn(Optional.of(recent));
    assertThatThrownBy(() -> service.send(7L, request))
        .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

    // 휴대폰 번호 형식이 아니면 400
    assertThatThrownBy(() -> service.send(7L, new PhoneVerificationDtos.SendRequest("02-123-4567")))
        .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void confirmVerifiesMemberAndWrongCodeIsCounted() {
    PhoneVerificationService service = service(new LocalPreviewPhoneCodeSender());
    PhoneVerificationChallenge issued = challenge(3L);
    when(repository.findById(3L)).thenReturn(Optional.of(issued));
    when(repository.findOwnedForUpdate(3L, 7L)).thenReturn(Optional.of(issued));

    when(encoder.matches("000000", "hashed")).thenReturn(false);
    assertThatThrownBy(
            () -> service.confirm(7L, new PhoneVerificationDtos.ConfirmRequest(3L, "000000")))
        .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    verify(attempts).recordFailure(3L, 7L, 5);
    assertThat(member.isPhoneVerified()).isFalse();

    when(encoder.matches("123456", "hashed")).thenReturn(true);
    PhoneVerificationDtos.StatusResponse result =
        service.confirm(7L, new PhoneVerificationDtos.ConfirmRequest(3L, "123456"));
    assertThat(result.phoneVerified()).isTrue();
    assertThat(result.maskedPhone()).isEqualTo("010-****-5678");
    assertThat(member.getPhone()).isEqualTo("01012345678");
    assertThat(issued.getConsumedAt()).isNotNull();

    // 한 번 쓴 인증번호는 다시 쓸 수 없습니다.
    assertThatThrownBy(
            () -> service.confirm(7L, new PhoneVerificationDtos.ConfirmRequest(3L, "123456")))
        .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void changingPhoneNumberResetsVerification() {
    member.verifyPhone("01012345678");
    // 같은 번호를 하이픈만 넣어 저장하면 인증은 그대로입니다.
    member.updateProfile("참여자", "010-1234-5678", null);
    assertThat(member.isPhoneVerified()).isTrue();
    // 다른 번호로 바꾸면 다시 인증해야 합니다.
    member.updateProfile("참여자", "010-9999-0000", null);
    assertThat(member.isPhoneVerified()).isFalse();
  }

  @Test
  void wrongCodeFiveTimesInvalidatesChallenge() {
    PhoneVerificationChallenge issued = challenge(4L);
    for (int i = 0; i < 5; i++) issued.recordFailedAttempt(5);
    assertThat(issued.isUsable(LocalDateTime.now())).isFalse();
  }
}
