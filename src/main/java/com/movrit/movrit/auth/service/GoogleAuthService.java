package com.movrit.movrit.auth.service;

import com.movrit.movrit.auth.dto.GoogleUserInfo;
import com.movrit.movrit.member.entity.AuthProvider;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberSocialAccount;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

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
        return getLoginUrl(null);
    }

    public String getLoginUrl(String state) {
        String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=openid%20email%20profile";

        return state == null ? url : url + "&state=" + state;
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
                            .role(MemberRole.USER)
                            .status(MemberStatus.ACTIVE)
                            .lastLoginAt(LocalDateTime.now())
                            .build();

                    Member savedMember = memberRepository.save(member);

                    socialAccountRepository.save(
                            MemberSocialAccount.builder()
                                    .member(savedMember)
                                    .provider(AuthProvider.GOOGLE)
                                    .providerId(providerId)
                                    .email(userInfo.getEmail())
                                    .build()
                    );

                    return savedMember;
                });
    }

    /**
     * 로그인한 MOVRIT 회원에게 구글 로그인 수단을 추가한다.
     * 이미 다른 회원에게 연결된 구글 계정은 자동 병합하지 않아 계정 탈취를 방지한다.
     */
    @Transactional
    public void linkAccount(Long memberId, GoogleUserInfo userInfo) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없어."));

        String providerId = userInfo.getProviderId();
        var existing = socialAccountRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId);

        if (existing.isPresent()) {
            if (existing.get().getMember().getId().equals(memberId)) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이 구글 계정은 이미 다른 MOVRIT 계정에 연결되어 있어.");
        }

        if (socialAccountRepository.existsByMemberIdAndProvider(memberId, AuthProvider.GOOGLE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 구글 계정이 연결되어 있어.");
        }

        socialAccountRepository.save(
                MemberSocialAccount.builder()
                        .member(member)
                        .provider(AuthProvider.GOOGLE)
                        .providerId(providerId)
                        .email(userInfo.getEmail())
                        .build()
        );
    }
}
