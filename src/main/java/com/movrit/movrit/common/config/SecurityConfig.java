package com.movrit.movrit.common.config;

import jakarta.servlet.DispatcherType;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
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
                // 실시간 채팅(SSE) 응답을 이어 보내는 비동기 처리 단계는 이미 인증된 요청이므로 다시 막지 않습니다.
                auth.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        "/api/members/signup",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/auth/kakao/**",
                        "/api/auth/google/**",
                        // 로그인 전 비밀번호 찾기·재설정
                        "/api/auth/password/**",
                        // 공개 이미지 폴더만 엽니다. 비공개 파일은 권한을 확인하는 API로만 내려갑니다.
                        "/uploads/campaigns/**",
                        "/uploads/brands/**",
                        "/uploads/products/**",
                        // API 문서(Swagger) 화면과 원본 JSON. 끄려면 API_DOCS_ENABLED=false로 실행합니다.
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/error")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/preorders/open",
                        "/api/preorders/browse",
                        "/api/preorders/*",
                        "/api/products/*/variants",
                        "/api/collaborations/campaigns/open",
                        // 캠페인 목록 검색·페이지(로그인 없이 볼 수 있습니다).
                        "/api/collaborations/campaigns/search",
                        // 커뮤니티 목록·상세는 로그인 없이 볼 수 있습니다.
                        "/api/community/posts",
                        "/api/community/posts/*",
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
                    // 캠페인 모집 현황(신청·선정 인원)도 로그인 없이 볼 수 있습니다.
                    .requestMatchers(
                        new RegexRequestMatcher(
                            "^/api/collaborations/campaigns/[0-9]+/recruitment$",
                            HttpMethod.GET.name()))
                    .permitAll()
                    // 구매평 캠페인의 구매처·판매가·환급액 안내도 로그인 없이 볼 수 있습니다.
                    .requestMatchers(
                        new RegexRequestMatcher(
                            "^/api/collaborations/campaigns/[0-9]+/purchase-review$",
                            HttpMethod.GET.name()))
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
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        // 보안 헤더: 다른 사이트의 iframe에 넣지 못하게, 내용 형식 추측 금지(기본값)에 더해
        // HTTPS로 접속한 브라우저는 1년 동안 HTTPS로만 오게 하고, 다른 사이트로 넘어갈 때 주소 전체를 보내지 않습니다.
        .headers(
            headers ->
                headers
                    .frameOptions(frame -> frame.deny())
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

    return http.build();
  }

  /**
   * 허용한 프론트 주소에서 오는 /api 요청만 받습니다. 주소는 movrit.security.allowed-origins(환경 변수 CORS_ALLOWED_ORIGINS,
   * 쉼표로 구분)에서 받으며, 기본값은 로컬 프론트(5173·5174)입니다. 모든 주소를 허용하는 '*'는 쓸 수 없습니다.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${movrit.security.allowed-origins}") List<String> allowedOrigins) {
    List<String> origins =
        allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
    if (origins.isEmpty() || origins.contains("*")) {
      throw new IllegalStateException("CORS 허용 주소를 정확히 지정해 주세요('*'는 쓸 수 없습니다).");
    }
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(origins);
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
    requireStrongJwtSecret(secret);
    SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
  }

  /**
   * HS256 서명 키는 32바이트(256비트) 이상이어야 합니다. 짧은 키로는 서버를 띄우지 않습니다. 키 값은 오류 문구·로그에 남기지 않습니다. 새 키 만들기 예:
   * openssl rand -hex 32
   */
  public static void requireStrongJwtSecret(String secret) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("JWT_SECRET은 32바이트 이상이어야 합니다(예: openssl rand -hex 32).");
    }
  }
}
