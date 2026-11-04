package com.movrit.movrit.phoneverification.service;

import java.util.regex.Pattern;

/** 휴대폰 번호 형식 검사·정리·가리기 규칙입니다(국내 휴대폰 010·011·016·017·018·019). */
public final class PhoneNumbers {
  private static final Pattern MOBILE = Pattern.compile("^01[016789][0-9]{7,8}$");

  private PhoneNumbers() {}

  /**
   * 하이픈·공백을 빼고 숫자만 남긴 번호를 돌려줍니다.
   *
   * @throws IllegalArgumentException 휴대폰 번호 형식이 아니면(안내 문구 포함)
   */
  public static String normalize(String value) {
    String digits = value == null ? "" : value.replaceAll("[\\s-]", "");
    if (!MOBILE.matcher(digits).matches()) {
      throw new IllegalArgumentException("휴대폰 번호를 확인해 주세요. 예: 010-1234-5678");
    }
    return digits;
  }

  /** 가운데를 가린 번호입니다. 예) 01012345678 → 010-****-5678 */
  public static String mask(String digits) {
    if (digits == null || digits.length() < 10) return null;
    return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
  }
}
