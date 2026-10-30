package com.movrit.movrit.member.entity;

/** 회원 계정의 이용 가능 상태입니다. ACTIVE가 아니면 로그인과 서비스 이용이 제한됩니다. */
public enum MemberStatus {
  /** 정상 이용 중인 계정입니다. */
  ACTIVE,
  /** 휴면 등으로 사용하지 않는 계정입니다. */
  INACTIVE,
  /** 운영 정책에 따라 이용이 정지된 계정입니다. */
  SUSPENDED,
  /** 회원 탈퇴한 계정입니다. */
  WITHDRAWN
}
