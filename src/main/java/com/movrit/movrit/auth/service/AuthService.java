package com.movrit.movrit.auth.service;

import com.movrit.movrit.auth.dto.LoginRequest;
import com.movrit.movrit.auth.dto.LoginResponse;
import com.movrit.movrit.auth.entity.RefreshToken;
import com.movrit.movrit.auth.jwt.JwtTokenProvider;
import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokens;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "이메일 또는 비밀번호가 올바르지 않아."
                ));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이용할 수 없는 회원 상태야."
            );
        }

        String passwordHash = member.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()
                || !passwordEncoder.matches(request.getPassword(), passwordHash)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "이메일 또는 비밀번호가 올바르지 않아."
            );
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getEmail()
        );
        String refreshToken = issueRefreshToken(member);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                member.getId(),
                member.getEmail(),
                member.getName()
        );
    }

    @Transactional
    public LoginResponse refresh(String raw) {
        RefreshToken saved = refreshTokens.findByTokenHash(hash(raw))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "유효하지 않은 Refresh Token이야."
                ));

        if (!saved.active()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "만료되거나 로그아웃된 Refresh Token이야."
            );
        }

        Member member = saved.getMember();
        String rotatedRefreshToken = issueRefreshToken(member);

        return new LoginResponse(
                jwtTokenProvider.createAccessToken(member.getId(), member.getEmail()),
                rotatedRefreshToken,
                "Bearer",
                member.getId(),
                member.getEmail(),
                member.getName()
        );
    }

    @Transactional
    public void logout(String raw) {
        refreshTokens.findByTokenHash(hash(raw)).ifPresent(RefreshToken::revoke);
    }

    private String issueRefreshToken(Member member) {
        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        String tokenHash = hash(raw);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);

        RefreshToken saved = refreshTokens.findByMemberId(member.getId()).orElse(null);
        if (saved == null) {
            refreshTokens.save(
                    RefreshToken.builder()
                            .member(member)
                            .tokenHash(tokenHash)
                            .expiresAt(expiresAt)
                            .build()
            );
        } else {
            saved.rotate(tokenHash, expiresAt);
        }

        return raw;
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(raw.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
