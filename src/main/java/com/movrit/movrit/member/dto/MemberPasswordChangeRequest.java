package com.movrit.movrit.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 현재 비밀번호를 확인한 뒤 새 비밀번호로 변경하는 요청입니다. */
public record MemberPasswordChangeRequest(
    @NotBlank @Size(max = 100) String currentPassword,
    @NotBlank
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해 주세요.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).+$",
            message = "비밀번호에는 영문자와 숫자를 각각 하나 이상 포함해 주세요.")
        String newPassword) {}
