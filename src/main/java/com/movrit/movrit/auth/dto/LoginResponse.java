package com.movrit.movrit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long memberId;
    private String email;
    private String name;
}
