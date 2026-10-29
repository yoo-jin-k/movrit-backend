package com.movrit.movrit.member.dto;

/** 계정이 현재 비밀번호 변경 기능을 사용할 수 있는지 알려줍니다. */
public record MemberPasswordCapabilityResponse(
    boolean hasPassword, boolean canChangePassword, String unavailableReason) {}
