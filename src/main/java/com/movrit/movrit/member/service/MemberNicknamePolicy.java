package com.movrit.movrit.member.service;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 회원 표시용 닉네임의 정규화와 허용 범위를 한 곳에서 관리합니다. */
public final class MemberNicknamePolicy {
  private static final Pattern ALLOWED = Pattern.compile("^[가-힣A-Za-z0-9_ ]+$");

  private MemberNicknamePolicy() {}

  public static String normalize(String value) {
    if (value == null) throw invalid();
    String normalized = value.trim().replaceAll("\\s+", " ");
    int length = normalized.codePointCount(0, normalized.length());
    if (length < 2 || length > 20 || !ALLOWED.matcher(normalized).matches()) throw invalid();
    return normalized;
  }

  /** 소셜 가입 화면이 닉네임을 받기 전까지 개인정보가 아닌 임시 표시명을 생성합니다. */
  public static String socialFallback() {
    return "social_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  }

  /** 매우 드문 임시 닉네임 충돌도 가입 전에 피할 수 있도록 사용 가능한 값을 찾습니다. */
  public static String availableSocialFallback(Predicate<String> alreadyExists) {
    String nickname;
    do {
      nickname = socialFallback();
    } while (alreadyExists.test(nickname));
    return nickname;
  }

  private static ResponseStatusException invalid() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "닉네임은 2~20자의 한글, 영문, 숫자, 밑줄과 단일 공백만 사용할 수 있습니다.");
  }
}
