package com.movrit.movrit.member.dto;

import com.movrit.movrit.member.entity.Member;

/** 회원 응답들이 동일한 버전 기반 프로필 사진 URL을 사용하도록 변환을 모읍니다. */
public final class MemberAvatarUrl {
  private MemberAvatarUrl() {}

  public static String from(Member member) {
    if (member.getAvatarKey() == null || member.getAvatarKey().isBlank()) return null;
    return "/api/members/" + member.getId() + "/avatar?v=" + member.getAvatarVersion();
  }
}
