package com.movrit.movrit.auth.controller;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.movrit.movrit.auth.jwt.JwtTokenProvider;
import com.movrit.movrit.auth.dto.KakaoUserInfo;
import com.movrit.movrit.auth.service.KakaoAuthService;
import com.movrit.movrit.member.entity.Member;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.movrit.movrit.auth.dto.LoginRequest;
import com.movrit.movrit.auth.dto.LoginResponse;
import com.movrit.movrit.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoAuthService kakaoAuthService;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<String> me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                "로그인 회원 ID: " + jwt.getSubject()
        );
    }

    @GetMapping("/kakao")
    public ResponseEntity<String> kakaoLogin(
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.redirect-uri}") String redirectUri
    ) {
        String kakaoAuthUrl =
                "https://kauth.kakao.com/oauth/authorize"
                        + "?response_type=code"
                        + "&client_id=" + clientId
                        + "&redirect_uri=" + redirectUri;

        return ResponseEntity.ok(kakaoAuthUrl);
    }

    @GetMapping("/kakao/callback")
    public void kakaoCallback(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) throws IOException {

        String kakaoAccessToken =
                kakaoAuthService.getAccessToken(code);

        KakaoUserInfo userInfo =
                kakaoAuthService.getUserInfo(kakaoAccessToken);

        Member member =
                kakaoAuthService.loginOrSignup(userInfo);

        String movritAccessToken =
                jwtTokenProvider.createAccessToken(
                        member.getId(),
                        member.getEmail()
                );

        String redirectUrl =
                "http://localhost:5173/oauth/callback"
                        + "?token=" + URLEncoder.encode(
                        movritAccessToken,
                        StandardCharsets.UTF_8
                );

        response.sendRedirect(redirectUrl);
    }
}
