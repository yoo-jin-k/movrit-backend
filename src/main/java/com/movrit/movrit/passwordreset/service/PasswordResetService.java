package com.movrit.movrit.passwordreset.service;

import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.passwordreset.dto.PasswordResetDtos;
import com.movrit.movrit.passwordreset.entity.PasswordResetToken;
import com.movrit.movrit.passwordreset.repository.PasswordResetTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 비밀번호 찾기(재설정 메일)와 새 비밀번호 저장을 처리합니다.
 *
 * <ul>
 *   <li>가입 여부를 알 수 없도록 어떤 이메일이든 같은 안내를 돌려줍니다.
 *   <li>재설정 링크는 30분 동안 한 번만 쓸 수 있고, 새로 요청하면 예전 링크는 막힙니다. 토큰 원문은 저장하지 않고 해시만 저장합니다.
 *   <li>1분 안에 다시 요청하거나 1시간에 5번을 넘으면 새 링크를 만들지 않습니다(안내는 같습니다).
 *   <li>메일 발송은 이메일 인증과 같은 설정(movrit.email.delivery)을 따릅니다. smtp면 메일을 보내고, local-preview(로컬 시연)면 화면에
 *       바로 재설정 버튼을 보여주도록 토큰을 응답에 담습니다.
 *   <li>비밀번호 없이 소셜로만 가입한 계정, 정지·탈퇴 계정은 링크를 만들지 않습니다.
 *   <li>재설정하면 로그인 유지 토큰을 끊어 다른 기기에서 다시 로그인하게 합니다.
 * </ul>
 */
@Slf4j
@Service
public class PasswordResetService {
  static final int EXPIRES_MINUTES = 30;
  static final String GENERIC_MESSAGE =
      "가입된 이메일이라면 비밀번호 재설정 안내를 보냈습니다. " + EXPIRES_MINUTES + "분 안에 링크를 열어 주세요.";

  private final MemberRepository memberRepository;
  private final PasswordResetTokenRepository tokenRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final ObjectProvider<JavaMailSender> mailSender;
  private final SecureRandom random = new SecureRandom();

  @Value("${movrit.email.delivery:unconfigured}")
  private String delivery = "unconfigured";

  @Value("${movrit.email.from:no-reply@movrit.local}")
  private String from = "no-reply@movrit.local";

  @Value("${app.frontend-base-url:http://localhost:5173}")
  private String frontendBaseUrl = "http://localhost:5173";

  public PasswordResetService(
      MemberRepository memberRepository,
      PasswordResetTokenRepository tokenRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      ObjectProvider<JavaMailSender> mailSender) {
    this.memberRepository = memberRepository;
    this.tokenRepository = tokenRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.mailSender = mailSender;
  }

  /** 재설정 메일을 요청합니다. 가입 여부와 관계없이 같은 안내를 돌려줍니다. */
  @Transactional
  public PasswordResetDtos.ForgotResponse requestReset(String email) {
    String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    Optional<Member> found = memberRepository.findByEmail(normalized).filter(this::canReset);
    if (found.isEmpty()) return new PasswordResetDtos.ForgotResponse(GENERIC_MESSAGE, null);
    Member member = found.get();
    LocalDateTime now = LocalDateTime.now();
    if (tokenRepository.countByMemberIdAndCreatedAtAfter(member.getId(), now.minusMinutes(1)) > 0
        || tokenRepository.countByMemberIdAndCreatedAtAfter(member.getId(), now.minusHours(1))
            >= 5) {
      return new PasswordResetDtos.ForgotResponse(GENERIC_MESSAGE, null);
    }
    // 새 링크를 보내면 예전 링크는 더 이상 쓰지 못하게 합니다.
    tokenRepository
        .findAllByMemberIdAndUsedAtIsNull(member.getId())
        .forEach(token -> token.markUsed(now));
    String rawToken = newToken();
    tokenRepository.save(
        PasswordResetToken.builder()
            .member(member)
            .tokenHash(hash(rawToken))
            .expiresAt(now.plusMinutes(EXPIRES_MINUTES))
            .build());
    return new PasswordResetDtos.ForgotResponse(GENERIC_MESSAGE, deliver(member, rawToken));
  }

  /** 링크가 아직 쓸 수 있는지 확인합니다. */
  @Transactional(readOnly = true)
  public PasswordResetDtos.ValidateResponse validate(String token) {
    return new PasswordResetDtos.ValidateResponse(findUsable(token).isPresent());
  }

  /**
   * 새 비밀번호를 저장합니다.
   *
   * @throws ResponseStatusException 링크가 만료·사용됐거나 없으면 400, 비밀번호가 72바이트를 넘으면 400
   */
  @Transactional
  public PasswordResetDtos.ResetResponse reset(String token, String newPassword) {
    PasswordResetToken resetToken =
        findUsable(token)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "만료되었거나 이미 사용한 재설정 링크입니다. 다시 요청해 주세요."));
    if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호가 너무 깁니다(72바이트 이내).");
    }
    Member member = resetToken.getMember();
    member.changePassword(passwordEncoder.encode(newPassword));
    resetToken.markUsed(LocalDateTime.now());
    refreshTokenRepository.findByMemberId(member.getId()).ifPresent(refresh -> refresh.revoke());
    return new PasswordResetDtos.ResetResponse("비밀번호를 바꿨습니다. 새 비밀번호로 로그인해 주세요.");
  }

  private Optional<PasswordResetToken> findUsable(String token) {
    if (token == null || token.isBlank() || token.length() > 200) return Optional.empty();
    LocalDateTime now = LocalDateTime.now();
    return tokenRepository
        .findByTokenHash(hash(token.trim()))
        .filter(value -> value.usable(now))
        .filter(value -> canReset(value.getMember()));
  }

  /** 비밀번호로 가입한 정상 계정만 재설정할 수 있습니다. */
  private boolean canReset(Member member) {
    return member.getDeletedAt() == null
        && member.getStatus() == MemberStatus.ACTIVE
        && member.getPasswordHash() != null
        && !member.getPasswordHash().isBlank();
  }

  /** 설정에 따라 메일을 보내거나(smtp), 로컬 시연 모드면 토큰을 돌려줍니다. 그 외에는 아무것도 보내지 않습니다. */
  private String deliver(Member member, String rawToken) {
    String link = frontendBaseUrl.replaceAll("/+$", "") + "/password/reset?token=" + rawToken;
    switch (delivery) {
      case "smtp" -> {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
          log.warn("비밀번호 재설정 메일을 보내지 못했습니다: 메일 서버 설정이 없습니다.");
          return null;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(member.getEmail());
        message.setSubject("[MOVRIT] 비밀번호 재설정 안내입니다.");
        message.setText(
            "아래 링크에서 새 비밀번호를 설정해 주세요. 링크는 "
                + EXPIRES_MINUTES
                + "분 동안 한 번만 쓸 수 있습니다.\n\n"
                + link
                + "\n\n본인이 요청하지 않았다면 이 메일을 무시해 주세요.");
        sender.send(message);
        return null;
      }
      case "local-preview" -> {
        // 로컬 시연 모드: 메일 대신 화면에 '재설정 테스트' 버튼을 보여줍니다(토큰 값은 로그에 남기지 않습니다).
        log.info("로컬 시연 모드: 비밀번호 재설정 링크를 화면 표시용으로 발급했습니다.");
        return rawToken;
      }
      default -> {
        log.warn("비밀번호 재설정 메일 발송 방식이 설정되지 않았습니다(movrit.email.delivery).");
        return null;
      }
    }
  }

  private String newToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
