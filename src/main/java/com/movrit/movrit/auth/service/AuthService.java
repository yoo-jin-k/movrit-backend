package com.movrit.movrit.auth.service;

import com.movrit.movrit.auth.dto.LoginRequest;
import com.movrit.movrit.auth.dto.LoginResponse;
import com.movrit.movrit.auth.jwt.JwtTokenProvider;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.movrit.movrit.auth.entity.RefreshToken;import com.movrit.movrit.auth.repository.RefreshTokenRepository;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.time.LocalDateTime;import java.util.HexFormat;import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokens;

    @Transactional public LoginResponse login(LoginRequest request) {

        // 1. 이메일로 회원 찾기
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException(
                        "이메일 또는 비밀번호가 올바르지 않아."
                ));

        // 2. 입력한 비밀번호와 DB의 암호화된 비밀번호 비교
        if (!passwordEncoder.matches(
                request.getPassword(),
                member.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 올바르지 않아."
            );
        }

        // 3. 이메일/비밀번호가 맞으면 JWT 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getEmail()
        );

        // 4. 프론트에 로그인 결과 반환
        String refreshToken=issueRefreshToken(member);
        return new LoginResponse(
                accessToken,refreshToken,
                "Bearer",
                member.getId(),
                member.getEmail(),
                member.getName()
        );
    }
    @Transactional public LoginResponse refresh(String raw){RefreshToken saved=refreshTokens.findByTokenHash(hash(raw)).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"유효하지 않은 Refresh Token이야."));if(!saved.active())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"만료되거나 로그아웃된 Refresh Token이야.");Member m=saved.getMember();String rotated=issueRefreshToken(m);return new LoginResponse(jwtTokenProvider.createAccessToken(m.getId(),m.getEmail()),rotated,"Bearer",m.getId(),m.getEmail(),m.getName());}
    @Transactional public void logout(String raw){refreshTokens.findByTokenHash(hash(raw)).ifPresent(RefreshToken::revoke);}
    private String issueRefreshToken(Member m){String raw=UUID.randomUUID()+"."+UUID.randomUUID();String h=hash(raw);LocalDateTime expires=LocalDateTime.now().plusDays(14);RefreshToken saved=refreshTokens.findByMemberId(m.getId()).orElse(null);if(saved==null)refreshTokens.save(RefreshToken.builder().member(m).tokenHash(h).expiresAt(expires).build());else saved.rotate(h,expires);return raw;}
    private String hash(String raw){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
