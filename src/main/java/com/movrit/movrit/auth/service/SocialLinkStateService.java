package com.movrit.movrit.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * 소셜 계정 연결 과정에서 "어떤 MOVRIT 회원이 연결을 시작했는지"를 안전하게 보존한다.
 *
 * 단순히 memberId를 state 파라미터에 넣으면 다른 회원 ID로 조작할 수 있기 때문에
 * 서버의 JWT 비밀키로 서명한 짧은 수명의 state 값을 사용한다.
 */
@Service
public class SocialLinkStateService {

    private static final long STATE_EXPIRATION_SECONDS = 600;

    private final byte[] secret;

    public SocialLinkStateService(@Value("${jwt.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String create(Long memberId) {
        long expiresAt = Instant.now().getEpochSecond() + STATE_EXPIRATION_SECONDS;
        String payload = memberId + ":" + expiresAt + ":" + UUID.randomUUID();
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        return encodedPayload + "." + sign(encodedPayload);
    }

    public Long getMemberId(String state) {
        if (state == null || state.isBlank()) {
            throw invalidState();
        }

        String[] parts = state.split("\\.");
        if (parts.length != 2) {
            throw invalidState();
        }

        String expectedSignature = sign(parts[0]);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[1].getBytes(StandardCharsets.UTF_8)
        )) {
            throw invalidState();
        }

        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(parts[0]),
                    StandardCharsets.UTF_8
            );
            String[] values = payload.split(":");

            Long memberId = Long.valueOf(values[0]);
            long expiresAt = Long.parseLong(values[1]);

            if (Instant.now().getEpochSecond() > expiresAt) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "소셜 계정 연결 시간이 만료됐어.");
            }

            return memberId;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw invalidState();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException("소셜 계정 연결 state 생성에 실패했어.", e);
        }
    }

    private ResponseStatusException invalidState() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 소셜 계정 연결 요청이야.");
    }
}
