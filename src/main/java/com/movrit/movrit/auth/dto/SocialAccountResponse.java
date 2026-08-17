package com.movrit.movrit.auth.dto;

import com.movrit.movrit.member.entity.MemberSocialAccount;

/** 마이페이지에서 회원에게 현재 연결된 로그인 수단을 보여주기 위한 응답 값이다. */
public record SocialAccountResponse(
        String provider,
        String email
) {
    public static SocialAccountResponse from(MemberSocialAccount account) {
        return new SocialAccountResponse(
                account.getProvider().name(),
                account.getEmail()
        );
    }
}
