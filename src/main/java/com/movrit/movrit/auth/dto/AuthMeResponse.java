package com.movrit.movrit.auth.dto;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;

/** 마이페이지와 관리자 화면에서 공통으로 사용할 로그인 회원 기본 정보 */
public record AuthMeResponse(
        Long id,
        String email,
        String name,
        MemberRole role
) {
    public static AuthMeResponse from(Member member) {
        return new AuthMeResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole()
        );
    }
}
