package com.movrit.movrit.member.entity;

/** 시스템 접근 권한입니다. 업체·참여자 같은 서비스 역할은 별도 프로필로 관리하고, 여기서는 일반 회원(USER)과 운영자(ADMIN)만 구분합니다. */
public enum MemberRole {
  /** 일반 회원입니다. 업체 기능은 승인된 업체 프로필이 있어야 사용할 수 있습니다. */
  USER,
  /** 운영자 화면과 심사·검수 기능을 사용할 수 있는 관리자입니다. */
  ADMIN
}
