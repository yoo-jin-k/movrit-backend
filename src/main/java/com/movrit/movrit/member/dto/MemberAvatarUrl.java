package com.movrit.movrit.member.dto;

import com.movrit.movrit.member.entity.Member;

/** 회원 응답들이 동일한 버전 기반 프로필 사진 URL을 사용하도록 변환을 모읍니다. */
public final class MemberAvatarUrl {
  private MemberAvatarUrl() {}

  /** 회원의 프로필 사진 주소를 만듭니다. 사진이 없으면 null입니다. */
  public static String from(Member member) {
    if (member.getAvatarKey() == null || member.getAvatarKey().isBlank()) return null;
    return "/api/members/" + member.getId() + "/avatar?v=" + member.getAvatarVersion();
  }
}
