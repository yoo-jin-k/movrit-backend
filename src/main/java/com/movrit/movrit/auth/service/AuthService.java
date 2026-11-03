package com.movrit.movrit.auth.service;

import com.movrit.movrit.auth.dto.LoginRequest;
import com.movrit.movrit.auth.dto.LoginResponse;
import com.movrit.movrit.auth.entity.RefreshToken;
import com.movrit.movrit.auth.jwt.JwtTokenProvider;
import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.member.dto.MemberAvatarUrl;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 이메일·비밀번호 로그인과 로그인 유지(리프레시 토큰 재발급), 로그아웃을 처리합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokens;
  private final LoginAttemptGuard attemptGuard;

  /**
   * 이메일과 비밀번호로 로그인합니다. 성공하면 접근 토큰과 리프레시 토큰을 발급합니다. 이메일이 없거나 비밀번호가 틀리면 같은 401 문구로 응답해 계정 존재 여부를 알 수
   * 없게 하고, 이용 정지 등 ACTIVE가 아닌 계정은 비밀번호가 맞을 때만 403으로 알려 줍니다(비밀번호 없이 계정 상태를 알아낼 수 없게). 같은 이메일이나 접속
   * 주소에서 실패가 너무 많으면 잠시 429로 거절합니다({@link LoginAttemptGuard}).
   */
  @Transactional
  public LoginResponse login(LoginRequest request) {
    return login(request, null);
  }

  /**
   * 접속 주소(IP)까지 함께 받아 로그인합니다. 컨트롤러는 이 메서드를 씁니다.
   *
   * @param clientAddress 요청한 접속 주소입니다. 모르면 null(이메일 기준 제한만 적용)
   */
  @Transactional
  public LoginResponse login(LoginRequest request, String clientAddress) {
    attemptGuard.check(request.getEmail(), clientAddress);

    // 1. 이메일로 회원 찾기, 2. 입력한 비밀번호와 DB의 암호화된 비밀번호 비교
    Member member = memberRepository.findByEmail(request.getEmail()).orElse(null);
    if (member == null
        || member.getPasswordHash() == null
        // BCrypt는 72바이트까지만 비교하므로 더 긴 비밀번호는 바로 거절해 앞부분만 같은 비밀번호가 통과하지 않게 합니다.
        || request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72
        || !passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
      attemptGuard.recordFailure(request.getEmail(), clientAddress);
      throw invalidCredentials();
    }
    attemptGuard.recordSuccess(request.getEmail());
    if (member.getStatus() != MemberStatus.ACTIVE)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이용할 수 없는 회원 상태입니다.");

    // 3. 이메일/비밀번호가 맞으면 JWT 생성
    String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail());

    // 4. 프론트에 로그인 결과 반환
    String refreshToken = issueRefreshToken(member);
    return new LoginResponse(
        accessToken,
        refreshToken,
        "Bearer",
        member.getId(),
        member.getEmail(),
        member.getName(),
        member.getNickname(),
        MemberAvatarUrl.from(member));
  }

  /** 리프레시 토큰으로 새 접근 토큰을 발급합니다. 사용한 리프레시 토큰은 새 값으로 교체(회전)됩니다. 없거나 만료·폐기된 토큰이면 401입니다. */
  @Transactional
  public LoginResponse refresh(String raw) {
    RefreshToken saved =
        refreshTokens
            .findByTokenHash(hash(raw))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."));
    if (!saved.active())
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "만료되거나 로그아웃된 Refresh Token입니다.");
    Member m = saved.getMember();
    String rotated = issueRefreshToken(m);
    return new LoginResponse(
        jwtTokenProvider.createAccessToken(m.getId(), m.getEmail()),
        rotated,
        "Bearer",
        m.getId(),
        m.getEmail(),
        m.getName(),
        m.getNickname(),
        MemberAvatarUrl.from(m));
  }

  /** 리프레시 토큰을 폐기해 더 이상 로그인 유지에 쓸 수 없게 합니다. 이미 없는 토큰이면 아무 일도 하지 않습니다. */
  @Transactional
  public void logout(String raw) {
    refreshTokens.findByTokenHash(hash(raw)).ifPresent(RefreshToken::revoke);
  }

  private String issueRefreshToken(Member m) {
    String raw = UUID.randomUUID() + "." + UUID.randomUUID();
    String h = hash(raw);
    LocalDateTime expires = LocalDateTime.now().plusDays(14);
    RefreshToken saved = refreshTokens.findByMemberId(m.getId()).orElse(null);
    if (saved == null)
      refreshTokens.save(RefreshToken.builder().member(m).tokenHash(h).expiresAt(expires).build());
    else saved.rotate(h, expires);
    return raw;
  }

  /** 이메일 존재 여부를 구분하지 않고 같은 응답을 사용해 계정 정보 노출을 막습니다. */
  private ResponseStatusException invalidCredentials() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
  }

  private String hash(String raw) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
