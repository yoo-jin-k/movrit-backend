package com.movrit.movrit.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/** MOVRIT 로그인 접근 토큰(JWT)을 만듭니다. 서버 비밀키(jwt.secret)로 HS256 서명합니다. */
@Component
public class JwtTokenProvider {

  private final JwtEncoder jwtEncoder;
  private final long accessTokenExpiration;

  /** 서명 비밀키와 토큰 유효 시간(밀리초)을 설정에서 받아 토큰 생성기를 준비합니다. 비밀키는 256비트 이상이어야 합니다. */
  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {
    SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    this.jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey).build();
    this.accessTokenExpiration = accessTokenExpiration;
  }

  /** 회원 ID를 subject로 담은 접근 토큰을 만듭니다. 이메일이 있으면 email 항목도 함께 넣습니다. */
  public String createAccessToken(Long memberId, String email) {

    Instant now = Instant.now();

    JwtClaimsSet.Builder builder =
        JwtClaimsSet.builder()
            .subject(String.valueOf(memberId))
            .issuedAt(now)
            .expiresAt(now.plusMillis(accessTokenExpiration));

    if (email != null) {
      builder.claim("email", email);
    }

    JwtClaimsSet claims = builder.build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
