package com.movrit.movrit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KakaoUserInfo {

    private Long providerId;
    private String nickname;
}
