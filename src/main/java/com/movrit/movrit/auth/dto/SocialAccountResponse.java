package com.movrit.movrit.auth.dto;

import com.movrit.movrit.member.entity.MemberSocialAccount;

/**
 * 마이페이지에서 회원에게 현재 연결된 로그인 수단을 보여주는 응답입니다. 로그인 및 소셜 인증의 연결된 소셜 로그인 수단을 전달합니다.
 *
 * @param provider 소셜 로그인 제공자 코드입니다.
 * @param email 이메일 주소입니다.
 */
public record SocialAccountResponse(String provider, String email) {
  /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
  public static SocialAccountResponse from(MemberSocialAccount account) {
    return new SocialAccountResponse(account.getProvider().name(), account.getEmail());
  }
}
