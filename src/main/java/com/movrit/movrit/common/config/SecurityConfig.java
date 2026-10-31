package com.movrit.movrit.common.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** API 보안 설정입니다. 로그인 없이 열어 둘 공개 주소와, JWT 로그인이 필요한 주소를 나누고 CORS·비밀번호 암호화·토큰 검증 방식을 정합니다. */
@Configuration
public class SecurityConfig {

  /** 비밀번호를 BCrypt로 암호화해 저장·비교하는 데 사용합니다. */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** 공개 API 목록과 인증 필요 API를 정하고, 세션 없이 JWT 토큰으로만 로그인 상태를 확인하도록 설정합니다. */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/members/signup",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/auth/kakao/**",
                        "/api/auth/google/**",
                        "/uploads/**",
                        "/error")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/preorders/open",
                        "/api/preorders/browse",
                        "/api/preorders/*",
                        "/api/products/*/variants",
                        "/api/collaborations/campaigns/open",
                        // 게시된 공지는 로그인 없이 볼 수 있습니다.
                        "/api/notices",
                        "/api/notices/*",
                        "/api/campaigns/*/application-form",
                        "/api/campaigns/*/eligibility-requirement")
                    .permitAll()
                    // 공개 상세는 숫자 ID만 허용해 /seller 같은 관리 경로가 열리지 않게 합니다.
                    .requestMatchers(
                        new RegexRequestMatcher(
                            "^/api/collaborations/campaigns/[0-9]+$", HttpMethod.GET.name()))
                    .permitAll()
                    .requestMatchers(
                        new RegexRequestMatcher(
                            "^/api/campaigns/[0-9]+/presentation$", HttpMethod.GET.name()))
                    .permitAll()
                    // 미디어 본문은 서비스에서 공개 캠페인과 현재 자산 연결을 매번 검사합니다.
                    .requestMatchers(
                        new RegexRequestMatcher(
                            "^/api/campaign-image-assets/[0-9a-f-]{36}/content$",
                            HttpMethod.GET.name()))
                    .permitAll()
                    .requestMatchers(
                        new RegexRequestMatcher(
                            "^/api/campaigns/[0-9]+/presentation/public$", HttpMethod.GET.name()))
                    .permitAll()
                    .requestMatchers(
                        new RegexRequestMatcher(
                            "^/api/members/[0-9]+/avatar(?:\\?.*)?$", HttpMethod.GET.name()))
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/collaborations/referrals/*/click")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

    return http.build();
  }

  /** 로컬 프론트 주소(5173·5174)에서 오는 /api 요청만 허용합니다. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  /** 요청에 담긴 JWT를 서버 비밀키(HS256)로 검증합니다. 서명이 다르거나 만료된 토큰은 거절됩니다. */
  @Bean
  public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
    SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
  }
}
