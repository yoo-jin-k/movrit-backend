package com.movrit.movrit.auth.service;

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import com.movrit.movrit.member.entity.AuthProvider;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.auth.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.movrit.movrit.member.entity.MemberSocialAccount;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;

import java.util.Map;

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

    public String getAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        body.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://kauth.kakao.com/oauth/token",
                request,
                Map.class
        );

        Map responseBody = response.getBody();
        return (String) responseBody.get("access_token");
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        Long providerId = ((Number) body.get("id")).longValue();

        String nickname = null;
        Map<String, Object> properties = (Map<String, Object>) body.get("properties");
        if (properties != null) {
            nickname = (String) properties.get("nickname");
        }

        return new KakaoUserInfo(providerId, nickname);
    }

    @Transactional
    public Member loginOrSignup(KakaoUserInfo userInfo) {
        String providerId = String.valueOf(userInfo.getProviderId());

        return socialAccountRepository
                .findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
                .map(socialAccount -> {
                    Member member = socialAccount.getMember();
                    member.updateLastLoginAt();
                    return member;
                })
                .orElseGet(() -> {
                    String nickname = userInfo.getNickname();
                    if (nickname == null || nickname.isBlank()) {
                        nickname = "카카오회원";
                    }

                    Member member = Member.builder()
                            .email(null)
                            .passwordHash(null)
                            .name(nickname)
                            .phone(null)
                            // 소셜 가입도 일반 가입과 동일하게 USER 권한으로 시작한다.
                            .role(MemberRole.USER)
                            .status(MemberStatus.ACTIVE)
                            .lastLoginAt(LocalDateTime.now())
                            .build();

                    Member savedMember = memberRepository.save(member);

                    MemberSocialAccount socialAccount = MemberSocialAccount.builder()
                            .member(savedMember)
                            .provider(AuthProvider.KAKAO)
                            .providerId(providerId)
                            .email(null)
                            .build();

                    socialAccountRepository.save(socialAccount);
                    return savedMember;
                });
    }
}
