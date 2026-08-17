package com.movrit.movrit.auth.jwt;

import com.movrit.movrit.member.entity.MemberRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final long accessTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration
    ) {
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey).build();
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String createAccessToken(Long memberId, String email, MemberRole role) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiresAt(now.plusMillis(accessTokenExpiration))
                .claim("role", role == null ? MemberRole.BUYER.name() : role.name());

        if (email != null) builder.claim("email", email);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, builder.build())).getTokenValue();
    }
}
