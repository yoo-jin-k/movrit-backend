package com.movrit.movrit.channelconnect.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 네이버 블로그 방문자 위젯이 쓰는 공개 주소(blog.naver.com/NVisitorgp4Ajax.nhn)에서 날짜별 방문자 수를 읽습니다.
 *
 * <ul>
 *   <li>위젯이 켜져 있으면 {@code <visitorcnt id="YYYYMMDD" cnt="N"/>} 형식으로 오늘을 포함한 5일 치가 옵니다.
 *   <li>위젯이 꺼져 있으면 내용 없는 응답(204)이 옵니다.
 * </ul>
 *
 * <p>요청 주소는 검사를 마친 블로그 아이디로 서버가 만듭니다. 응답은 짧게 기다리고, 실패하면 '지금 읽을 수 없음'으로 돌려줍니다.
 */
@Slf4j
@Component
public class HttpNaverBlogVisitorReader implements NaverBlogVisitorReader {
  private static final Pattern VISITOR =
      Pattern.compile("<visitorcnt\\s+id=\"(\\d{8})\"\\s+cnt=\"(\\d+)\"");
  private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

  private final RestClient http;

  public HttpNaverBlogVisitorReader() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(3));
    factory.setReadTimeout(Duration.ofSeconds(5));
    this.http = RestClient.builder().requestFactory(factory).build();
  }

  @Override
  public VisitorReadResult read(String blogId) {
    try {
      ResponseEntity<String> response =
          http.get()
              .uri("https://blog.naver.com/NVisitorgp4Ajax.nhn?blogId={id}", blogId)
              .retrieve()
              .toEntity(String.class);
      if (response.getStatusCode().value() == 204) return VisitorReadResult.widgetOff();
      return parse(response.getBody());
    } catch (RestClientException exception) {
      log.warn("블로그 방문자 수 확인 실패: error={}", exception.getClass().getSimpleName());
      return VisitorReadResult.unavailable();
    }
  }

  /** 위젯 응답에서 날짜별 방문자 수를 꺼냅니다. 본문이 비어 있으면 위젯이 꺼진 것으로 봅니다. */
  static VisitorReadResult parse(String body) {
    if (body == null || body.isBlank()) return VisitorReadResult.widgetOff();
    Map<LocalDate, Long> days = new LinkedHashMap<>();
    Matcher matcher = VISITOR.matcher(body);
    while (matcher.find()) {
      try {
        days.put(LocalDate.parse(matcher.group(1), DAY), Long.parseLong(matcher.group(2)));
      } catch (DateTimeParseException | NumberFormatException ignored) {
        // 형식이 다른 항목은 건너뜁니다.
      }
    }
    return days.isEmpty() ? VisitorReadResult.unavailable() : VisitorReadResult.found(days);
  }
}
