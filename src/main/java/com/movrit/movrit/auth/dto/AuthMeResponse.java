package com.movrit.movrit.auth.dto;

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
 */
public record AuthMeResponse(Long id, String email, String name, String nickname, MemberRole role) {
  /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
  public static AuthMeResponse from(Member member) {
    return new AuthMeResponse(
        member.getId(),
        member.getEmail(),
        member.getName(),
        member.getNickname(),
        member.getRole());
  }
}
