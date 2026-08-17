package com.movrit.movrit.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.movrit.movrit.auth.dto.GoogleUserInfo;
import com.movrit.movrit.member.entity.AuthProvider;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.movrit.movrit.member.entity.MemberSocialAccount;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final MemberSocialAccountRepository socialAccountRepository;
    private final MemberRepository memberRepository;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    public String getLoginUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=openid%20email%20profile";
    }

    public String getAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token",
                request,
                Map.class
        );

        return (String) response.getBody().get("access_token");
    }

    public GoogleUserInfo getUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://openidconnect.googleapis.com/v1/userinfo",
                HttpMethod.GET,
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();

        String providerId = (String) body.get("sub");
        String email = (String) body.get("email");
        String name = (String) body.get("name");

        return new GoogleUserInfo(providerId, email, name);
    }

    @Transactional
    public Member loginOrSignup(GoogleUserInfo userInfo) {
        String providerId = userInfo.getProviderId();

        return socialAccountRepository
                .findByProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .map(socialAccount -> {
                    Member member = socialAccount.getMember();
                    member.updateLastLoginAt();
                    return member;
                })
                .orElseGet(() -> {
                    String name = userInfo.getName();
                    if (name == null || name.isBlank()) {
                        name = "구글회원";
                    }

                    Member member = Member.builder()
                            .email(userInfo.getEmail())
                            .passwordHash(null)
                            .name(name)
                            .phone(null)
                            // 소셜 가입 경로로 관리자 권한이 생성되지 않도록 USER로 고정한다.
                            .role(MemberRole.USER)
                            .status(MemberStatus.ACTIVE)
                            .lastLoginAt(LocalDateTime.now())
                            .build();

                    Member savedMember = memberRepository.save(member);

                    MemberSocialAccount socialAccount = MemberSocialAccount.builder()
                            .member(savedMember)
                            .provider(AuthProvider.GOOGLE)
                            .providerId(providerId)
                            .email(userInfo.getEmail())
                            .build();

                    socialAccountRepository.save(socialAccount);
                    return savedMember;
                });
    }
}
