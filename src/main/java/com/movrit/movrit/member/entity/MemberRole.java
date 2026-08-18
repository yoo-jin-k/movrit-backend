package com.movrit.movrit.member.entity;

/**
 * MOVRIT 회원 역할.
 * 구매자(BUYER)를 기본 역할로 두고, 판매자 승인 시 BRAND,
 * 인플루언서 승인 시 CREATOR, 운영 계정은 ADMIN을 사용한다.
 */
public enum MemberRole {
    BUYER,
    CREATOR,
    BRAND,
    ADMIN
}
