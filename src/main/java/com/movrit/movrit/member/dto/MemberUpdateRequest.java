package com.movrit.movrit.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원의 회원 정보 수정 요청을 전달합니다.
 *
 * @param name 표시 이름입니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 max = 50입니다.
 * @param phone 연락처입니다. 크기 제약은 max = 20입니다.
 * @param nickname 공개 닉네임입니다. 생략하면 기존 닉네임을 유지합니다.
 */
public record MemberUpdateRequest(
    @NotBlank @Size(max = 50) String name, @Size(max = 20) String phone, String nickname) {}
