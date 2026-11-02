package com.movrit.movrit.auth.controller;

import com.movrit.movrit.auth.dto.AuthMeResponse;
import com.movrit.movrit.auth.dto.GoogleUserInfo;
import com.movrit.movrit.auth.dto.KakaoUserInfo;
import com.movrit.movrit.auth.dto.LoginRequest;
import com.movrit.movrit.auth.dto.LoginResponse;
import com.movrit.movrit.auth.dto.SocialAccountResponse;
import com.movrit.movrit.auth.jwt.JwtTokenProvider;
import com.movrit.movrit.auth.service.AuthService;
import com.movrit.movrit.auth.service.GoogleAuthService;
import com.movrit.movrit.auth.service.KakaoAuthService;
import com.movrit.movrit.auth.service.SocialAccountService;
import com.movrit.movrit.auth.service.SocialLinkStateService;
import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** 로그인 및 소셜 인증의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@Tag(name = "공통 · 로그인·소셜 인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
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

  /** 이메일과 비밀번호로 로그인하고 접근 토큰을 받습니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  /**
   * 로그인 및 소셜 인증의 로그인 세션 재발급 또는 종료 요청을 전달합니다.
   *
   * @param refreshToken 접근 토큰 재발급에 사용하는 토큰이며 로그에 노출하지 않습니다.
   */
  public record RefreshRequest(String refreshToken) {}

  /** 재발급 토큰으로 로그인 토큰 재발급을 요청합니다. */
  @PostMapping("/refresh")
  public LoginResponse refresh(@RequestBody RefreshRequest r) {
    return authService.refresh(r.refreshToken());
  }

  /** 재발급 토큰의 로그아웃 처리를 요청하고 본문 없는 응답을 반환합니다. */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestBody RefreshRequest r) {
    authService.logout(r.refreshToken());
    return ResponseEntity.noContent().build();
  }

  /** Google 로그인 화면으로 이동할 인증 URL을 반환합니다. */
  @GetMapping("/google/login-url")
  public ResponseEntity<String> googleLoginUrl() {
    return ResponseEntity.ok(googleAuthService.getLoginUrl());
  }

  /** Kakao 로그인 화면으로 이동할 인증 URL을 반환합니다. */
  @GetMapping("/kakao")
  public ResponseEntity<String> kakaoLogin() {
    return ResponseEntity.ok(kakaoAuthService.getLoginUrl(null));
  }

  /** 로그인 회원 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/me")
  public ResponseEntity<AuthMeResponse> me(@AuthenticationPrincipal Jwt jwt) {
    Long memberId = JwtMemberId.from(jwt);
    return ResponseEntity.ok(AuthMeResponse.from(memberService.getMember(memberId)));
  }

  /** 현재 MOVRIT 계정에 연결된 소셜 로그인 수단을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/social/accounts")
  public List<SocialAccountResponse> socialAccounts(@AuthenticationPrincipal Jwt jwt) {
    return socialAccountService.findByMemberId(JwtMemberId.from(jwt));
  }

  /**
   * 로그인된 회원이 카카오 계정을 추가 연결할 때 사용하는 인증 URL입니다. state에는 회원 ID를 그대로 노출하지 않고 서버 서명을 포함한 일회성 값을 넣습니다.
   *
   * <p>기존 회원 계정 연결을 위한 서명된 state를 포함한 Kakao 인증 URL을 반환합니다. 요청한 회원은 로그인 토큰으로 확인합니다.
   */
  @GetMapping("/social/kakao/link-url")
  public ResponseEntity<String> kakaoLinkUrl(@AuthenticationPrincipal Jwt jwt) {
    String state = socialLinkStateService.create(JwtMemberId.from(jwt));
    return ResponseEntity.ok(kakaoAuthService.getLoginUrl(state));
  }

  /** 기존 회원 계정 연결을 위한 서명된 state를 포함한 Google 인증 URL을 반환합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/social/google/link-url")
  public ResponseEntity<String> googleLinkUrl(@AuthenticationPrincipal Jwt jwt) {
    String state = socialLinkStateService.create(JwtMemberId.from(jwt));
    return ResponseEntity.ok(googleAuthService.getLoginUrl(state));
  }

  /** Kakao 인증 코드를 교환합니다. state가 있으면 계정 연결을 처리하고, 없으면 로그인 후 프론트로 이동합니다. */
  @GetMapping("/kakao/callback")
  public void kakaoCallback(
      @RequestParam("code") String code,
      @RequestParam(value = "state", required = false) String state,
      HttpServletResponse response)
      throws IOException {
    // 외부 토큰 교환·사용자 조회가 실패하면(키·Secret·Redirect URI 설정 오류 등) 500 오류 화면 대신
    // 로그인 화면으로 돌려보내 안내합니다. 인가 코드와 토큰 값은 로그에 남기지 않습니다.
    KakaoUserInfo userInfo;
    try {
      String kakaoAccessToken = kakaoAuthService.getAccessToken(code);
      userInfo = kakaoAuthService.getUserInfo(kakaoAccessToken);
    } catch (RestClientException e) {
      log.warn("KAKAO 소셜 로그인 외부 요청 실패: {}", e.getClass().getSimpleName());
      redirectSocialLoginFailure(response, "KAKAO");
      return;
    }

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

  /** Google 인증 코드를 교환합니다. state가 있으면 계정 연결을 처리하고, 없으면 로그인 후 프론트로 이동합니다. */
  @GetMapping("/google/callback")
  public void googleCallback(
      @RequestParam("code") String code,
      @RequestParam(value = "state", required = false) String state,
      HttpServletResponse response)
      throws IOException {
    // 외부 토큰 교환·사용자 조회가 실패하면(키·Secret·Redirect URI 설정 오류 등) 500 오류 화면 대신
    // 로그인 화면으로 돌려보내 안내합니다. 인가 코드와 토큰 값은 로그에 남기지 않습니다.
    GoogleUserInfo userInfo;
    try {
      String googleAccessToken = googleAuthService.getAccessToken(code);
      userInfo = googleAuthService.getUserInfo(googleAccessToken);
    } catch (RestClientException e) {
      log.warn("GOOGLE 소셜 로그인 외부 요청 실패: {}", e.getClass().getSimpleName());
      redirectSocialLoginFailure(response, "GOOGLE");
      return;
    }

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

    // 같은 이메일의 기존 회원이 있으면 새 계정을 만들지 않고 로그인 화면에서 연결 방법을 안내합니다.
    Member member;
    try {
      member = googleAuthService.loginOrSignup(userInfo);
    } catch (ResponseStatusException e) {
      redirectSocialLoginFailure(response, "GOOGLE_EMAIL_EXISTS");
      return;
    }
    redirectLogin(response, member);
  }

  /** MOVRIT 접근 토큰을 발급하고 시작 시 검증한 프론트 주소로 이동합니다. 토큰 포함 주소를 로그에 기록하지 않습니다. */
  private void redirectLogin(HttpServletResponse response, Member member) throws IOException {
    String movritAccessToken =
        jwtTokenProvider.createAccessToken(member.getId(), member.getEmail());

    String redirectUrl =
        frontendBaseUrl
            + "/oauth/callback?token="
            + URLEncoder.encode(movritAccessToken, StandardCharsets.UTF_8);

    response.sendRedirect(redirectUrl);
  }

  /** 소셜 로그인 실패를 로그인 화면에 전달합니다. 화면은 provider 값만 보고 안내 문구를 고르며 서버 오류 상세는 전달하지 않습니다. */
  private void redirectSocialLoginFailure(HttpServletResponse response, String provider)
      throws IOException {
    response.sendRedirect(frontendBaseUrl + "/login?socialError=" + provider);
  }

  /** 계정 연결 결과를 프론트에 전달합니다. 결과 메시지는 쿼리 문자열에 넣기 전에 인코딩합니다. */
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
