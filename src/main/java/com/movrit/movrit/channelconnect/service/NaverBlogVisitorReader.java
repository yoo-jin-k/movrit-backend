package com.movrit.movrit.channelconnect.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 네이버 블로그의 방문자 위젯이 공개하는 날짜별 방문자 수를 읽습니다. 블로그 주인이 '방문자' 위젯을 켜 둔 경우에만 값이 나옵니다.
 *
 * <p>회원이 신청 화면에서 '지금 확인'을 누를 때만 호출합니다. 매일 자동으로 다시 읽지 않습니다.
 */
public interface NaverBlogVisitorReader {

  /**
   * 날짜별 방문자 수를 읽습니다. 블로그 아이디는 형식 검사를 마친 값만 넘깁니다.
   *
   * @param blogId 네이버 블로그 아이디
   */
  VisitorReadResult read(String blogId);

  /** 읽은 결과의 종류입니다. */
  enum Status {
    /** 날짜별 방문자 수를 읽었습니다. */
    FOUND,
    /** 방문자 위젯이 꺼져 있어 네이버가 값을 주지 않았습니다. */
    WIDGET_OFF,
    /** 지금은 읽을 수 없습니다(통신 실패, 형식이 다른 응답 등). */
    UNAVAILABLE
  }

  /**
   * @param status 결과 종류
   * @param dailyVisitors 날짜별 방문자 수(FOUND일 때만). 네이버는 오늘을 포함한 최근 5일을 줍니다.
   */
  record VisitorReadResult(Status status, Map<LocalDate, Long> dailyVisitors) {
    public static VisitorReadResult found(Map<LocalDate, Long> dailyVisitors) {
      return new VisitorReadResult(Status.FOUND, Map.copyOf(dailyVisitors));
    }

    public static VisitorReadResult widgetOff() {
      return new VisitorReadResult(Status.WIDGET_OFF, Map.of());
    }

    public static VisitorReadResult unavailable() {
      return new VisitorReadResult(Status.UNAVAILABLE, Map.of());
    }
  }
}
