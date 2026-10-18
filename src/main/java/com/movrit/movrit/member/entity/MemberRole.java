package com.movrit.movrit.member.entity;

/**
 * MOVRIT 시스템 권한을 나타냅니다. 구매자·판매자·크리에이터 같은 서비스 역할은 별도 프로필/관계로 관리하고, 여기서는 시스템 접근 권한만 USER와 ADMIN으로
 * 구분합니다.
 */
public enum MemberRole {
  USER,
  ADMIN
}
