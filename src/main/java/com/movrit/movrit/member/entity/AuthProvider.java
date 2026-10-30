package com.movrit.movrit.member.entity;

/** 회원이 로그인에 사용하는 수단입니다. 한 회원에게 여러 소셜 수단을 연결할 수 있습니다. */
public enum AuthProvider {
  /** 이메일과 비밀번호로 가입·로그인합니다. */
  LOCAL,
  /** 카카오 계정으로 로그인합니다. */
  KAKAO,
  /** Google 계정으로 로그인합니다. */
  GOOGLE
}
