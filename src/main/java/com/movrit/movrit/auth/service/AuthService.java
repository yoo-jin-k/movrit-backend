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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {

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
        return new LoginResponse(
                accessToken,
                "Bearer",
                member.getId(),
                member.getEmail(),
                member.getName()
        );
    }
}
