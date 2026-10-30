package com.movrit.movrit.emailverification.service;

import com.movrit.movrit.emailverification.dto.EmailVerificationDtos;
import com.movrit.movrit.emailverification.entity.EmailVerificationChallenge;
import com.movrit.movrit.emailverification.repository.EmailVerificationChallengeRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 회원 이메일 소유 확인(6자리 인증 번호)을 처리합니다. 재발송 간격·시간당 횟수·틀린 입력 횟수를 제한하며, 인증을 마쳐야 캠페인에 신청할 수 있습니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {
  private static final SecureRandom RANDOM = new SecureRandom();

  private final EmailVerificationChallengeRepository repository;
  private final MemberService memberService;
  private final PasswordEncoder passwordEncoder;
  private final VerificationCodeDelivery delivery;
  private final EmailVerificationAttemptService attemptService;

  @Value("${movrit.email.verification.expires-minutes:10}")
  private int expiresMinutes;

  @Value("${movrit.email.verification.resend-seconds:60}")
  private int resendSeconds;

  @Value("${movrit.email.verification.hourly-limit:5}")
  private int hourlyLimit;

  @Value("${movrit.email.verification.max-attempts:5}")
  private int maxAttempts;

  /**
   * 로그인한 회원 이메일로 새 인증 번호를 보냅니다. 이전에 보낸 미사용 번호는 모두 무효화합니다. 너무 빨리 다시 요청하거나 한 시간 한도를 넘으면 429, 이메일이 없으면
   * 400입니다.
   */
  @Transactional
  public EmailVerificationDtos.RequestResponse request(Long memberId) {
    Member member = memberService.getMember(memberId);
    if (member.getEmail() == null || member.getEmail().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증할 이메일을 먼저 등록해 주세요.");
    }

    LocalDateTime now = LocalDateTime.now();
    repository
        .findFirstByMemberIdOrderByIdDesc(memberId)
        .ifPresent(
            latest -> {
              if (latest.getCreatedAt().isAfter(now.minusSeconds(resendSeconds))) {
                throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "인증 메일 재발송은 잠시 후 다시 시도해 주세요.");
              }
            });
    if (repository.countByMemberIdAndCreatedAtAfter(memberId, now.minusHours(1)) >= hourlyLimit) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "한 시간 동안 요청할 수 있는 인증 메일 횟수를 초과했습니다.");
    }

    repository
        .findAllByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(memberId)
        .forEach(EmailVerificationChallenge::invalidate);
    String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
    EmailVerificationChallenge challenge =
        repository.save(
            EmailVerificationChallenge.builder()
                .member(member)
                .emailSnapshot(member.getEmail())
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(now.plusMinutes(expiresMinutes))
                .attemptCount(0)
                .build());

    // 발송에 실패하면 트랜잭션도 롤백해 사용할 수 없는 요청을 남기지 않습니다.
    delivery.send(member.getEmail(), code, expiresMinutes);
    return new EmailVerificationDtos.RequestResponse(
        challenge.getId(), mask(member.getEmail()), challenge.getExpiresAt());
  }

  /**
   * 입력한 인증 번호를 확인합니다. 본인 요청이 아니면 404, 요청 후 이메일이 바뀌었거나 만료·사용된 번호면 409, 번호가 틀리면 실패 횟수를 기록하고 400입니다.
   * 성공하면 회원을 인증 완료로 바꿉니다.
   */
  @Transactional
  public EmailVerificationDtos.StatusResponse confirm(
      Long memberId, EmailVerificationDtos.ConfirmRequest request) {
    Member member = memberService.getMember(memberId);
    EmailVerificationChallenge observed =
        repository
            .findById(request.challengeId())
            .filter(challenge -> challenge.getMember().getId().equals(memberId))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."));
    // 인증 요청 뒤 이메일을 바꿨다면 예전 주소로 받은 번호로 새 주소가 인증되지 않도록 요청을 무효화합니다.
    if (!observed.getEmailSnapshot().equalsIgnoreCase(member.getEmail())) {
      attemptService.invalidate(request.challengeId(), memberId);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이메일이 변경되어 인증 요청이 무효화되었습니다.");
    }
    if (!observed.isUsable(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "만료되었거나 이미 사용된 인증 요청입니다.");
    }
    if (!passwordEncoder.matches(request.code(), observed.getCodeHash())) {
      attemptService.recordFailure(request.challengeId(), memberId, maxAttempts);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 번호가 올바르지 않습니다.");
    }

    EmailVerificationChallenge challenge =
        repository
            .findOwnedForUpdate(request.challengeId(), memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."));
    if (!challenge.isUsable(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "만료되었거나 이미 사용된 인증 요청입니다.");
    }
    challenge.consume();
    member.verifyEmail();
    return status(memberId);
  }

  /** 로그인한 회원의 이메일 인증 여부와 인증 시각을 반환합니다. 휴대폰 실명 인증은 아직 없으므로 임시 이메일 정책(EMAIL_TEMPORARY)으로 표시합니다. */
  public EmailVerificationDtos.StatusResponse status(Long memberId) {
    Member member = memberService.getMember(memberId);
    return new EmailVerificationDtos.StatusResponse(
        member.isEmailVerified(), member.getEmailVerifiedAt(), false, "EMAIL_TEMPORARY");
  }

  private String mask(String email) {
    int at = email.indexOf('@');
    if (at <= 1) {
      return "***" + email.substring(Math.max(at, 0));
    }
    return email.substring(0, 1) + "***" + email.substring(at);
  }
}
