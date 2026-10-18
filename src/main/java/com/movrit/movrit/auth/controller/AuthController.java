package com.movrit.movrit.auth.controller;

import com.movrit.movrit.auth.dto.*;
import com.movrit.movrit.auth.jwt.JwtTokenProvider;
import com.movrit.movrit.auth.service.*;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final GoogleAuthService googleAuthService;
  private final JwtTokenProvider jwtTokenProvider;
  private final KakaoAuthService kakaoAuthService;
  private final AuthService authService;
  private final MemberService memberService;
  private final SocialLinkStateService socialLinkStateService;
  private final SocialAccountService socialAccountService;

  @Value("${app.frontend-base-url}")
  private String frontendBaseUrl;

  private static final Set<String> ALLOWED_FRONTEND_BASE_URLS =
      Set.of("http://localhost:5173", "http://localhost:5174");

  /** OAuth 토큰이 승인되지 않은 주소로 전달되지 않도록 시작 시점에 확인합니다. */
  @PostConstruct
  void validateFrontendBaseUrl() {
    if (!ALLOWED_FRONTEND_BASE_URLS.contains(frontendBaseUrl)) {
      throw new IllegalStateException("허용되지 않은 프론트 OAuth 리디렉션 주소입니다.");
    }
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  public record RefreshRequest(String refreshToken) {}

  @PostMapping("/refresh")
  public LoginResponse refresh(@RequestBody RefreshRequest r) {
    return authService.refresh(r.refreshToken());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestBody RefreshRequest r) {
    authService.logout(r.refreshToken());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/google/login-url")
  public ResponseEntity<String> googleLoginUrl() {
    return ResponseEntity.ok(googleAuthService.getLoginUrl());
  }

  @GetMapping("/kakao")
  public ResponseEntity<String> kakaoLogin() {
    return ResponseEntity.ok(kakaoAuthService.getLoginUrl(null));
  }

  @GetMapping("/me")
  public ResponseEntity<AuthMeResponse> me(@AuthenticationPrincipal Jwt jwt) {
    Long memberId = Long.valueOf(jwt.getSubject());
    return ResponseEntity.ok(AuthMeResponse.from(memberService.getMember(memberId)));
  }

  /** 현재 MOVRIT 계정에 연결된 소셜 로그인 수단을 조회합니다. */
  @GetMapping("/social/accounts")
  public List<SocialAccountResponse> socialAccounts(@AuthenticationPrincipal Jwt jwt) {
    return socialAccountService.findByMemberId(Long.valueOf(jwt.getSubject()));
  }

  /**
   * 로그인된 회원이 카카오 계정을 추가 연결할 때 사용하는 인증 URL입니다. state에는 회원 ID를 그대로 노출하지 않고 서버 서명을 포함한 일회성 값을 넣습니다.
   */
  @GetMapping("/social/kakao/link-url")
  public ResponseEntity<String> kakaoLinkUrl(@AuthenticationPrincipal Jwt jwt) {
    String state = socialLinkStateService.create(Long.valueOf(jwt.getSubject()));
    return ResponseEntity.ok(kakaoAuthService.getLoginUrl(state));
  }

  @GetMapping("/social/google/link-url")
  public ResponseEntity<String> googleLinkUrl(@AuthenticationPrincipal Jwt jwt) {
    String state = socialLinkStateService.create(Long.valueOf(jwt.getSubject()));
    return ResponseEntity.ok(googleAuthService.getLoginUrl(state));
  }

  @GetMapping("/kakao/callback")
  public void kakaoCallback(
      @RequestParam("code") String code,
      @RequestParam(value = "state", required = false) String state,
      HttpServletResponse response)
      throws IOException {
    String kakaoAccessToken = kakaoAuthService.getAccessToken(code);
    KakaoUserInfo userInfo = kakaoAuthService.getUserInfo(kakaoAccessToken);

    // state가 있으면 신규 로그인/회원가입이 아니라 기존 회원의 계정 연결 요청입니다.
    if (state != null && !state.isBlank()) {
      try {
        Long memberId = socialLinkStateService.getMemberId(state);
        kakaoAuthService.linkAccount(memberId, userInfo);
        redirectSocialLink(response, "KAKAO", true, null);
      } catch (ResponseStatusException e) {
        redirectSocialLink(response, "KAKAO", false, e.getReason());
      }
      return;
    }

    Member member = kakaoAuthService.loginOrSignup(userInfo);
    redirectLogin(response, member);
  }

  @GetMapping("/google/callback")
  public void googleCallback(
      @RequestParam("code") String code,
      @RequestParam(value = "state", required = false) String state,
      HttpServletResponse response)
      throws IOException {
    String googleAccessToken = googleAuthService.getAccessToken(code);
    GoogleUserInfo userInfo = googleAuthService.getUserInfo(googleAccessToken);

    if (state != null && !state.isBlank()) {
      try {
        Long memberId = socialLinkStateService.getMemberId(state);
        googleAuthService.linkAccount(memberId, userInfo);
        redirectSocialLink(response, "GOOGLE", true, null);
      } catch (ResponseStatusException e) {
        redirectSocialLink(response, "GOOGLE", false, e.getReason());
      }
      return;
    }

    Member member = googleAuthService.loginOrSignup(userInfo);
    redirectLogin(response, member);
  }

  private void redirectLogin(HttpServletResponse response, Member member) throws IOException {
    String movritAccessToken =
        jwtTokenProvider.createAccessToken(member.getId(), member.getEmail());

    String redirectUrl =
        frontendBaseUrl
            + "/oauth/callback?token="
            + URLEncoder.encode(movritAccessToken, StandardCharsets.UTF_8);

    response.sendRedirect(redirectUrl);
  }

  private void redirectSocialLink(
      HttpServletResponse response, String provider, boolean success, String message)
      throws IOException {
    StringBuilder url =
        new StringBuilder(frontendBaseUrl + "/social-link/callback")
            .append("?provider=")
            .append(provider)
            .append("&success=")
            .append(success);

    if (message != null && !message.isBlank()) {
      url.append("&message=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    response.sendRedirect(url.toString());
  }
}
