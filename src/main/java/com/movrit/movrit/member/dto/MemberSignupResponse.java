package com.movrit.movrit.member.dto;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;

public record MemberSignupResponse(
    Long id, String email, String name, String phone, MemberStatus status) {
  public static MemberSignupResponse from(Member member) {
    return new MemberSignupResponse(
        member.getId(), member.getEmail(), member.getName(), member.getPhone(), member.getStatus());
  }
}
