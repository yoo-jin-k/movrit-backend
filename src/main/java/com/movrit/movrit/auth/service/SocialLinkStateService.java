package com.movrit.movrit.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 소셜 계정 연결 과정에서 "어떤 MOVRIT 회원이 연결을 시작했는지"를 안전하게 보존합니다.
 *
 * <p>단순히 memberId를 state 파라미터에 넣으면 다른 회원 ID로 조작할 수 있기 때문에 서버의 JWT 비밀키로 서명한 짧은 수명의 state 값을 사용합니다.
 */
@Service
public class SocialLinkStateService {

  private static final long STATE_EXPIRATION_SECONDS = 600;

  private final byte[] secret;

  /** JWT 비밀키를 서명 키로 사용합니다. */
  public SocialLinkStateService(@Value("${jwt.secret}") String secret) {
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  /** 소셜 계정 연결을 시작한 회원 ID를 담은 일회성 state 값을 만듭니다. 10분 뒤 만료되며 서버 서명이 붙어 다른 값으로 바꿀 수 없습니다. */
  public String create(Long memberId) {
    long expiresAt = Instant.now().getEpochSecond() + STATE_EXPIRATION_SECONDS;
    String payload = memberId + ":" + expiresAt + ":" + UUID.randomUUID();
    String encodedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

    return encodedPayload + "." + sign(encodedPayload);
  }

  /** 콜백으로 돌아온 state의 서명과 만료를 확인하고 연결을 시작한 회원 ID를 꺼냅니다. 서명이 다르거나 형식이 틀리거나 만료되면 401로 거절합니다. */
  public Long getMemberId(String state) {
    if (state == null || state.isBlank()) {
      throw invalidState();
    }

    String[] parts = state.split("\\.");
    if (parts.length != 2) {
      throw invalidState();
    }

    String expectedSignature = sign(parts[0]);
    // 서명 비교는 길이·내용에 따라 걸리는 시간이 달라지지 않는 방식으로 해 추측 공격을 막습니다.
    if (!MessageDigest.isEqual(
        expectedSignature.getBytes(StandardCharsets.UTF_8),
        parts[1].getBytes(StandardCharsets.UTF_8))) {
      throw invalidState();
    }

    try {
      String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
      String[] values = payload.split(":");

      Long memberId = Long.valueOf(values[0]);
      long expiresAt = Long.parseLong(values[1]);

      if (Instant.now().getEpochSecond() > expiresAt) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "소셜 계정 연결 시간이 만료되었습니다.");
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
      throw new IllegalStateException("소셜 계정 연결 state 생성에 실패했습니다.", e);
    }
  }

  private ResponseStatusException invalidState() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 소셜 계정 연결 요청입니다.");
  }
}
