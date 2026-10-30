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
 * @param avatarUrl 재인코딩된 프로필 사진 URL이며 사진이 없으면 null입니다.
 */
public record MemberSignupResponse(
    Long id,
    String email,
    String name,
    String nickname,
    String phone,
    MemberStatus status,
    String avatarUrl) {
  /** 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
  public static MemberSignupResponse from(Member member) {
    return new MemberSignupResponse(
        member.getId(),
        member.getEmail(),
        member.getName(),
        member.getNickname(),
        member.getPhone(),
        member.getStatus(),
        MemberAvatarUrl.from(member));
  }
}
