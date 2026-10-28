package com.movrit.movrit.member.dto;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;

/**
 * 회원의 회원 정보 응답을 전달합니다.
 *
 * @param id 대상 데이터의 식별자입니다.
 * @param email 이메일 주소입니다.
 * @param name 표시 이름입니다.
 * @param nickname 서비스에서 공개할 닉네임입니다.
 * @param phone 연락처입니다.
 * @param status 현재 업무 상태입니다.
 */
public record MemberSignupResponse(
    Long id, String email, String name, String nickname, String phone, MemberStatus status) {
  /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
  public static MemberSignupResponse from(Member member) {
    return new MemberSignupResponse(
        member.getId(),
        member.getEmail(),
        member.getName(),
        member.getNickname(),
        member.getPhone(),
        member.getStatus());
  }
}
