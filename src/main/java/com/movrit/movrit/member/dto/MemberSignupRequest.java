package com.movrit.movrit.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberSignupRequest(
        @NotBlank(message = "이메일은 필수야.")
        @Email(message = "이메일 형식이 올바르지 않아.")
        String email,

        @NotBlank(message = "비밀번호는 필수야.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해줘.")
        String password,

        @NotBlank(message = "이름은 필수야.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해줘.")
        String name,

        @Pattern(regexp = "^$|^[0-9+\\-]{8,20}$", message = "전화번호 형식이 올바르지 않아.")
        String phone
) {
}
