package com.movrit.movrit.phoneverification.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.phoneverification.dto.PhoneVerificationDtos;
import com.movrit.movrit.phoneverification.entity.PhoneVerificationChallenge;
import com.movrit.movrit.phoneverification.repository.PhoneVerificationChallengeRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 휴대폰 번호 인증(6자리 인증번호)을 처리합니다. 흐름: 번호 입력 → 인증번호 발송 → 입력한 번호 확인 → 회원 연락처 '인증됨'.
 *
 * <ul>
 *   <li>인증번호는 해시로만 저장하고 5분 동안 쓸 수 있습니다. 새로 보내면 이전 번호는 무효화합니다.
 *   <li>남용 제한: 재발송 60초 간격, 회원당 1시간 5번, 같은 번호로 하루 10번(회원과 무관), 틀린 입력 5번이면 그 인증번호는 무효.
 *   <li>다른 회원이 이미 인증한 번호는 409입니다(한 사람이 여러 계정으로 패널티·신청 제한을 피하지 못하게).
 *   <li>로컬 시연 모드(local-preview)에서만 인증번호를 응답에 담아 화면에 보여줍니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhoneVerificationService {
  private static final SecureRandom RANDOM = new SecureRandom();

  private final PhoneVerificationChallengeRepository repository;
  private final MemberService memberService;
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final PhoneCodeSender sender;
  private final PhoneVerificationAttemptService attemptService;

  @Value("${movrit.phone.verification.expires-minutes:5}")
  private int expiresMinutes;

  @Value("${movrit.phone.verification.resend-seconds:60}")
  private int resendSeconds;

  @Value("${movrit.phone.verification.hourly-limit:5}")
  private int hourlyLimit;

  @Value("${movrit.phone.verification.number-daily-limit:10}")
  private int numberDailyLimit;

  @Value("${movrit.phone.verification.max-attempts:5}")
  private int maxAttempts;

  /**
   * 입력한 번호로 인증번호를 보냅니다.
   *
   * @throws ResponseStatusException 번호 형식 오류 400, 다른 회원이 인증한 번호 409, 너무 잦은 요청 429, 발송 설정 없음 503
   */
  @Transactional
  public PhoneVerificationDtos.SendResponse send(
      Long memberId, PhoneVerificationDtos.SendRequest request) {
    Member member = memberService.getMember(memberId);
    String phone = normalize(request.phone());
    requireNotTaken(phone, memberId);

    LocalDateTime now = LocalDateTime.now();
    repository
        .findFirstByMemberIdOrderByIdDesc(memberId)
        .filter(latest -> latest.getCreatedAt().isAfter(now.minusSeconds(resendSeconds)))
        .ifPresent(
            latest -> {
              throw new ResponseStatusException(
                  HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 뒤에 다시 받을 수 있습니다.");
            });
    if (repository.countByMemberIdAndCreatedAtAfter(memberId, now.minusHours(1)) >= hourlyLimit) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "한 시간 동안 받을 수 있는 인증번호 횟수를 넘었습니다. 잠시 후 다시 시도해 주세요.");
    }
    if (repository.countByPhoneAndCreatedAtAfter(phone, now.minusDays(1)) >= numberDailyLimit) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "이 번호로 오늘 보낼 수 있는 인증번호 횟수를 넘었습니다. 내일 다시 시도해 주세요.");
    }

    repository
        .findAllByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(memberId)
        .forEach(PhoneVerificationChallenge::invalidate);
    String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
    PhoneVerificationChallenge challenge =
        repository.save(
            PhoneVerificationChallenge.builder()
                .member(member)
                .phone(phone)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(now.plusMinutes(expiresMinutes))
                .attemptCount(0)
                .build());
    // 보내지 못하면 트랜잭션을 되돌려 쓸 수 없는 요청을 남기지 않습니다.
    sender.send(phone, code, expiresMinutes);
    String localPreviewCode = sender.exposesCodeForLocalPreview() ? code : null;
    return new PhoneVerificationDtos.SendResponse(
        challenge.getId(), PhoneNumbers.mask(phone), challenge.getExpiresAt(), localPreviewCode);
  }

  /**
   * 입력한 인증번호를 확인하고 회원 연락처를 '인증됨'으로 바꿉니다.
   *
   * @throws ResponseStatusException 본인 요청이 아니면 404, 만료·사용·무효화된 요청 또는 그사이 다른 회원이 인증한 번호면 409, 인증번호가
   *     틀리면 400(틀린 횟수는 따로 저장)
   */
  @Transactional
  public PhoneVerificationDtos.StatusResponse confirm(
      Long memberId, PhoneVerificationDtos.ConfirmRequest request) {
    Member member = memberService.getMember(memberId);
    PhoneVerificationChallenge observed =
        repository
            .findById(request.challengeId())
            .filter(challenge -> challenge.getMember().getId().equals(memberId))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."));
    if (!observed.isUsable(LocalDateTime.now())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "만료되었거나 이미 사용한 인증번호입니다. 인증번호를 다시 받아 주세요.");
    }
    if (!passwordEncoder.matches(request.code(), observed.getCodeHash())) {
      attemptService.recordFailure(request.challengeId(), memberId, maxAttempts);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다.");
    }
    PhoneVerificationChallenge challenge =
        repository
            .findOwnedForUpdate(request.challengeId(), memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."));
    if (!challenge.isUsable(LocalDateTime.now())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "만료되었거나 이미 사용한 인증번호입니다. 인증번호를 다시 받아 주세요.");
    }
    requireNotTaken(challenge.getPhone(), memberId);
    challenge.consume();
    member.verifyPhone(challenge.getPhone());
    try {
      // 같은 번호를 두 회원이 동시에 인증하면 유일 조건(uk_member_verified_phone)이 하나만 남깁니다.
      memberRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw taken();
    }
    return status(memberId);
  }

  /** 로그인한 회원의 휴대폰 인증 상태입니다. */
  public PhoneVerificationDtos.StatusResponse status(Long memberId) {
    Member member = memberService.getMember(memberId);
    return new PhoneVerificationDtos.StatusResponse(
        member.isPhoneVerified(),
        member.getPhoneVerifiedAt(),
        member.isPhoneVerified() ? PhoneNumbers.mask(member.getPhone()) : null);
  }

  private void requireNotTaken(String phone, Long memberId) {
    if (memberRepository.existsByPhoneAndPhoneVerifiedAtIsNotNullAndIdNot(phone, memberId)) {
      throw taken();
    }
  }

  private static ResponseStatusException taken() {
    return new ResponseStatusException(
        HttpStatus.CONFLICT, "이미 다른 회원이 인증한 번호입니다. 본인 번호가 맞다면 고객센터로 문의해 주세요.");
  }

  private static String normalize(String phone) {
    try {
      return PhoneNumbers.normalize(phone);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
  }
}
