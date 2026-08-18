package com.movrit.movrit.auth.service;

import com.movrit.movrit.auth.dto.PasswordResetDtos;
import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long EXPIRES_SECONDS = 30 * 60;
    private static final String GENERIC_MESSAGE =
            "가입된 이메일이라면 비밀번호 재설정 안내를 보냈어.";

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.password-reset.frontend-url:http://localhost:5173/password/reset}")
    private String resetFrontendUrl;

    @Value("${app.password-reset.expose-token:false}")
    private boolean exposeToken;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Transactional(readOnly = true)
    public PasswordResetDtos.ForgotResponse forgot(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        Member member = memberRepository.findByEmail(email).orElse(null);

        if (member == null || member.getStatus() != MemberStatus.ACTIVE) {
            return new PasswordResetDtos.ForgotResponse(GENERIC_MESSAGE, null);
        }

        String token = issueToken(member);
        deliver(member.getEmail(), token);

        return new PasswordResetDtos.ForgotResponse(
                GENERIC_MESSAGE,
                exposeToken ? token : null
        );
    }

    @Transactional(readOnly = true)
    public PasswordResetDtos.ValidateResponse validate(String token) {
        try {
            resolveMember(token);
            return new PasswordResetDtos.ValidateResponse(true);
        } catch (RuntimeException e) {
            return new PasswordResetDtos.ValidateResponse(false);
        }
    }

    @Transactional
    public PasswordResetDtos.MessageResponse reset(String token, String newPassword) {
        Member member = resolveMember(token);

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw invalidToken();
        }

        String currentHash = member.getPasswordHash();
        if (currentHash != null && passwordEncoder.matches(newPassword, currentHash)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "기존 비밀번호와 다른 비밀번호를 사용해줘."
            );
        }

        member.changePassword(passwordEncoder.encode(newPassword));
        refreshTokenRepository.findByMemberId(member.getId()).ifPresent(tokenEntity -> {
            tokenEntity.revoke();
        });

        return new PasswordResetDtos.MessageResponse(
                "비밀번호를 변경했어. 새 비밀번호로 다시 로그인해줘."
        );
    }

    private String issueToken(Member member) {
        long expiresAt = Instant.now().getEpochSecond() + EXPIRES_SECONDS;
        String payload = member.getId() + ":" + expiresAt + ":" + passwordFingerprint(member);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    private Member resolveMember(String token) {
        if (token == null || token.isBlank()) {
            throw invalidToken();
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) {
            throw invalidToken();
        }

        byte[] expected = sign(parts[0]).getBytes(StandardCharsets.UTF_8);
        byte[] actual = parts[1].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw invalidToken();
        }

        final String decoded;
        try {
            decoded = new String(
                    Base64.getUrlDecoder().decode(parts[0]),
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException e) {
            throw invalidToken();
        }

        String[] payload = decoded.split(":", -1);
        if (payload.length != 3) {
            throw invalidToken();
        }

        final Long memberId;
        final long expiresAt;
        try {
            memberId = Long.valueOf(payload[0]);
            expiresAt = Long.parseLong(payload[1]);
        } catch (NumberFormatException e) {
            throw invalidToken();
        }

        if (Instant.now().getEpochSecond() > expiresAt) {
            throw invalidToken();
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(this::invalidToken);

        if (!MessageDigest.isEqual(
                payload[2].getBytes(StandardCharsets.UTF_8),
                passwordFingerprint(member).getBytes(StandardCharsets.UTF_8)
        )) {
            // 비밀번호가 이미 바뀐 경우 기존 링크를 1회용처럼 무효화한다.
            throw invalidToken();
        }

        return member;
    }

    private String passwordFingerprint(Member member) {
        String source = member.getPasswordHash();
        if (source == null || source.isBlank()) {
            source = "NO_LOCAL_PASSWORD:" + member.getId();
        }
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(source.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    ("password-reset:" + jwtSecret).getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("비밀번호 재설정 토큰 생성에 실패했어.", e);
        }
    }

    private void deliver(String email, String token) {
        String url = resetFrontendUrl + "?token=" + token;

        if (!mailEnabled) {
            log.info("[MOVRIT password reset][development] email={}, url={}", email, url);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom);
            }
            message.setTo(email);
            message.setSubject("[MOVRIT] 비밀번호 재설정");
            message.setText(
                    "MOVRIT 비밀번호를 재설정하려면 아래 링크를 열어줘.\n\n" +
                    url + "\n\n이 링크는 30분 동안 유효하고 한 번 사용하면 다시 사용할 수 없어."
            );
            mailSender.send(message);
        } catch (MailException e) {
            // 메일 장애 때문에 존재하는 이메일 여부가 외부 응답으로 노출되지 않도록 한다.
            log.error("비밀번호 재설정 메일 발송 실패: {}", email, e);
        }
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "만료되었거나 유효하지 않은 비밀번호 재설정 링크야."
        );
    }
}
