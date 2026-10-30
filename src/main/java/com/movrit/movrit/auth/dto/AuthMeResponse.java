package com.movrit.movrit.auth.dto;

import com.movrit.movrit.member.dto.MemberAvatarUrl;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;

/**
 * 마이페이지와 관리자 화면에서 공통으로 사용할 로그인 회원 기본 정보를 반환합니다. 로그인 및 소셜 인증의 로그인 회원 정보를 전달합니다.
 *
 * @param id 대상 데이터의 식별자입니다.
 * @param email 이메일 주소입니다.
 * @param name 표시 이름입니다.
 * @param nickname 서비스에서 공개할 닉네임입니다.
 * @param role 서버가 부여한 회원 권한입니다.
 * @param avatarUrl 재인코딩된 프로필 사진 URL이며 사진이 없으면 null입니다.
 */
public record AuthMeResponse(
    Long id, String email, String name, String nickname, MemberRole role, String avatarUrl) {
  /** 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
  public static AuthMeResponse from(Member member) {
    return new AuthMeResponse(
        member.getId(),
        member.getEmail(),
        member.getName(),
        member.getNickname(),
        member.getRole(),
        MemberAvatarUrl.from(member));
  }
}
