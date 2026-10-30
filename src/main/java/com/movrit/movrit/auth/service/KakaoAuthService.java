package com.movrit.movrit.auth.service;

import com.movrit.movrit.auth.dto.KakaoUserInfo;
import com.movrit.movrit.member.entity.AuthProvider;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberSocialAccount;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import com.movrit.movrit.member.service.MemberNicknamePolicy;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * 카카오 로그인을 처리합니다. 카카오 인증 화면 주소를 만들고, 돌아온 인가 코드로 토큰·사용자 정보를 받아 MOVRIT 회원으로 로그인·가입시키거나 기존 계정에 연결합니다.
 */
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

  private final MemberSocialAccountRepository socialAccountRepository;
  private final MemberRepository memberRepository;

  @Value("${kakao.client-id}")
  private String clientId;

  @Value("${kakao.client-secret}")
  private String clientSecret;

  @Value("${kakao.redirect-uri}")
  private String redirectUri;

  /** 카카오 로그인 동의 화면 주소를 만듭니다. state가 있으면(계정 연결) 함께 붙입니다. */
  public String getLoginUrl(String state) {
    String url =
        "https://kauth.kakao.com/oauth/authorize"
            + "?response_type=code"
            + "&client_id="
            + clientId
            + "&redirect_uri="
            + redirectUri;

    return state == null ? url : url + "&state=" + state;
  }

  /**
   * Kakao 토큰 교환 요청 본문을 만듭니다.
   *
   * <p>Kakao Developers에서 Client Secret 사용을 끈 앱은 secret 값이 없으므로, 값이 비어 있으면 client_secret 항목을 아예 보내지
   * 않습니다. 사용을 켠 앱은 설정된 값을 그대로 전달합니다. redirect_uri는 인가 요청과 반드시 같은 값이어야 합니다.
   */
  MultiValueMap<String, String> tokenRequestBody(String code) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "authorization_code");
    body.add("client_id", clientId);
    body.add("redirect_uri", redirectUri);
    body.add("code", code);
    if (clientSecret != null && !clientSecret.isBlank()) {
      body.add("client_secret", clientSecret);
    }
    return body;
  }

  /** 카카오가 돌려준 인가 코드를 카카오 접근 토큰으로 바꿉니다. 키·Secret·Redirect URI가 맞지 않으면 카카오가 오류(401 등)를 돌려줍니다. */
  public String getAccessToken(String code) {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> request =
        new HttpEntity<>(tokenRequestBody(code), headers);

    ResponseEntity<Map> response =
        restTemplate.postForEntity("https://kauth.kakao.com/oauth/token", request, Map.class);

    return (String) response.getBody().get("access_token");
  }

  /** 카카오 접근 토큰으로 사용자 고유 ID와 닉네임을 가져옵니다. 이메일은 받지 않습니다. */
  public KakaoUserInfo getUserInfo(String accessToken) {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    HttpEntity<Void> request = new HttpEntity<>(headers);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "https://kapi.kakao.com/v2/user/me", HttpMethod.GET, request, Map.class);

    Map<String, Object> body = response.getBody();
    Long providerId = ((Number) body.get("id")).longValue();

    String nickname = null;
    Map<String, Object> properties = (Map<String, Object>) body.get("properties");
    if (properties != null) {
      nickname = (String) properties.get("nickname");
    }

    return new KakaoUserInfo(providerId, nickname);
  }

  /** 카카오 계정이 이미 연결된 회원이면 로그인시키고, 처음이면 새 회원(임시 닉네임)으로 가입시킵니다. */
  @Transactional
  public Member loginOrSignup(KakaoUserInfo userInfo) {
    String providerId = String.valueOf(userInfo.getProviderId());

    return socialAccountRepository
        .findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
        .map(
            socialAccount -> {
              Member member = socialAccount.getMember();
              member.updateLastLoginAt();
              return member;
            })
        .orElseGet(
            () -> {
              String nickname = userInfo.getNickname();
              if (nickname == null || nickname.isBlank()) {
                nickname = "카카오회원";
              }

              Member member =
                  Member.builder()
                      .email(null)
                      .passwordHash(null)
                      .name(nickname)
                      .nickname(
                          MemberNicknamePolicy.availableSocialFallback(
                              memberRepository::existsByNickname))
                      .phone(null)
                      .role(MemberRole.USER)
                      .status(MemberStatus.ACTIVE)
                      .lastLoginAt(LocalDateTime.now())
                      .build();

              Member savedMember = memberRepository.save(member);

              socialAccountRepository.save(
                  MemberSocialAccount.builder()
                      .member(savedMember)
                      .provider(AuthProvider.KAKAO)
                      .providerId(providerId)
                      .email(null)
                      .build());

              return savedMember;
            });
  }

  /** 로그인한 MOVRIT 회원에게 카카오 로그인 수단을 추가합니다. 이미 다른 회원에게 연결된 카카오 계정은 자동 이전하지 않습니다. */
  @Transactional
  public void linkAccount(Long memberId, KakaoUserInfo userInfo) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));

    String providerId = String.valueOf(userInfo.getProviderId());
    var existing =
        socialAccountRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId);

    if (existing.isPresent()) {
      if (existing.get().getMember().getId().equals(memberId)) {
        return;
      }
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "이 카카오 계정은 이미 다른 MOVRIT 계정에 연결되어 있습니다.");
    }

    if (socialAccountRepository.existsByMemberIdAndProvider(memberId, AuthProvider.KAKAO)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 카카오 계정이 연결되어 있습니다.");
    }

    socialAccountRepository.save(
        MemberSocialAccount.builder()
            .member(member)
            .provider(AuthProvider.KAKAO)
            .providerId(providerId)
            .email(null)
            .build());
  }
}
