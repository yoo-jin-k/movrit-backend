package com.movrit.movrit.emailverification.service;

import com.movrit.movrit.emailverification.dto.EmailVerificationDtos;
import com.movrit.movrit.emailverification.entity.EmailVerificationChallenge;
import com.movrit.movrit.emailverification.repository.EmailVerificationChallengeRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

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

    @Transactional
    public EmailVerificationDtos.RequestResponse request(Long memberId) {
        Member member = memberService.getMember(memberId);
        if (member.getEmail() == null || member.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증할 이메일을 먼저 등록해 주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        repository.findFirstByMemberIdOrderByIdDesc(memberId).ifPresent(latest -> {
            if (latest.getCreatedAt().isAfter(now.minusSeconds(resendSeconds))) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "인증 메일 재발송은 잠시 후 다시 시도해 주세요.");
            }
        });
        if (repository.countByMemberIdAndCreatedAtAfter(memberId, now.minusHours(1)) >= hourlyLimit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "한 시간 동안 요청할 수 있는 인증 메일 횟수를 초과했습니다.");
        }

        repository.findAllByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(memberId)
                .forEach(EmailVerificationChallenge::invalidate);
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        EmailVerificationChallenge challenge = repository.save(EmailVerificationChallenge.builder()
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

    @Transactional
    public EmailVerificationDtos.StatusResponse confirm(
            Long memberId,
            EmailVerificationDtos.ConfirmRequest request
    ) {
        Member member = memberService.getMember(memberId);
        EmailVerificationChallenge observed = repository.findById(request.challengeId())
                .filter(challenge -> challenge.getMember().getId().equals(memberId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."));
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

        EmailVerificationChallenge challenge = repository
                .findOwnedForUpdate(request.challengeId(), memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."));
        if (!challenge.isUsable(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "만료되었거나 이미 사용된 인증 요청입니다.");
        }
        challenge.consume();
        member.verifyEmail();
        return status(memberId);
    }

    public EmailVerificationDtos.StatusResponse status(Long memberId) {
        Member member = memberService.getMember(memberId);
        return new EmailVerificationDtos.StatusResponse(
                member.isEmailVerified(),
                member.getEmailVerifiedAt(),
                false,
                "EMAIL_TEMPORARY"
        );
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.substring(0, 1) + "***" + email.substring(at);
    }
}
