package com.movrit.movrit.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(
    @NotBlank @Size(max = 50) String name, @Size(max = 20) String phone) {}
