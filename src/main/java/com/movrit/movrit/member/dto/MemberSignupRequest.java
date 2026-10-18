package com.movrit.movrit.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원의 회원가입 요청을 전달합니다.
 *
 * @param email 이메일 주소입니다. 빈 문자열을 허용하지 않습니다.
 * @param password 입력 비밀번호이며 로그와 응답에 노출하지 않습니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 min = 8, max = 100, message
 *     = 비밀번호는 8자 이상 100자 이하로 입력해 주세요.입니다.
 * @param name 표시 이름입니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 max = 50, message = 이름은 50자 이하로 입력해 주세요.입니다.
 * @param phone 연락처입니다.
 */
public record MemberSignupRequest(
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "이메일 형식이 올바르지 않아.") String email,
    @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해 주세요.")
        String password,
    @NotBlank(message = "이름은 필수입니다.") @Size(max = 50, message = "이름은 50자 이하로 입력해 주세요.") String name,
    @Pattern(regexp = "^$|^[0-9+\\-]{8,20}$", message = "전화번호 형식이 올바르지 않아.") String phone) {}
