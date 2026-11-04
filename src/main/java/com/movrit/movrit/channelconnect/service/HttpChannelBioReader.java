package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/**
 * 소개글을 SNS가 공식으로 열어 둔 곳에서만 읽습니다. 화면(HTML)을 긁어 오지 않습니다.
 *
 * <ul>
 *   <li>네이버 블로그: 공개 RSS(rss.blog.naver.com/{아이디}.xml)의 채널 설명이 블로그 소개글입니다. 블로그 제목이 비어 있으면 없는 블로그로
 *       봅니다.
 *   <li>인스타그램: 메타 Graph API의 Business Discovery로 비즈니스·크리에이터 계정의 소개글과 팔로워 수를 읽습니다. 우리 서비스의 인스타그램
 *       비즈니스 계정 ID와 접근 토큰(환경 변수)이 있어야 하며, 없거나 개인 계정이면 '자동 확인 불가'로 돌려 운영자 확인으로 넘깁니다.
 * </ul>
 *
 * <p>요청 주소는 검사를 마친 아이디로 서버가 만듭니다(회원이 입력한 주소를 그대로 열지 않음). 응답은 짧게 기다리고, 토큰은 로그에 남기지 않습니다.
 */
@Slf4j
@Component
public class HttpChannelBioReader implements ChannelBioReader {
  private static final Pattern CHANNEL =
      Pattern.compile("<channel>(.*?)(?:<item>|</channel>)", Pattern.DOTALL);
  private static final Pattern TITLE =
      Pattern.compile("<title>(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?</title>", Pattern.DOTALL);
  private static final Pattern DESCRIPTION =
      Pattern.compile("<description>(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?</description>", Pattern.DOTALL);

  private final ChannelConnectSettings settings;
  private final RestClient http;

  public HttpChannelBioReader(ChannelConnectSettings settings) {
    this.settings = settings;
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(3));
    factory.setReadTimeout(Duration.ofSeconds(5));
    this.http = RestClient.builder().requestFactory(factory).build();
  }

  @Override
  public boolean autoCheck(ChannelProvider provider) {
    return switch (provider) {
      case NAVER_BLOG -> true;
      case INSTAGRAM -> settings.instagramDiscoveryConfigured();
      default -> false;
    };
  }

  @Override
  public BioReadResult read(ChannelProvider provider, String accountId) {
    try {
      return switch (provider) {
        case NAVER_BLOG -> naverBlog(accountId);
        case INSTAGRAM -> instagram(accountId);
        default -> BioReadResult.unavailable();
      };
    } catch (RestClientException | IllegalStateException exception) {
      log.warn("소개글 확인 실패: provider={}, error={}", provider, exception.getClass().getSimpleName());
      return BioReadResult.unavailable();
    }
  }

  private BioReadResult naverBlog(String blogId) {
    String body =
        http.get().uri("https://rss.blog.naver.com/{id}.xml", blogId).retrieve().body(String.class);
    return parseNaverRss(body);
  }

  /** 네이버 블로그 RSS에서 블로그 제목과 소개글을 꺼냅니다. 제목이 비어 있으면 없는 블로그입니다. */
  static BioReadResult parseNaverRss(String body) {
    if (body == null || !body.contains("<rss")) return BioReadResult.unavailable();
    Matcher channel = CHANNEL.matcher(body);
    if (!channel.find()) return BioReadResult.notFound();
    String head = channel.group(1);
    String title = first(TITLE, head);
    if (title.isBlank()) return BioReadResult.notFound();
    return BioReadResult.found(unescape(first(DESCRIPTION, head)), null, unescape(title));
  }

  private BioReadResult instagram(String username) {
    if (!settings.instagramDiscoveryConfigured()) return BioReadResult.unavailable();
    try {
      JsonNode response =
          http.get()
              .uri(
                  "https://graph.facebook.com/{version}/{account}?fields={fields}&access_token={token}",
                  settings.instagramGraphVersion(),
                  settings.instagramDiscoveryAccountId(),
                  "business_discovery.username("
                      + username
                      + "){username,biography,followers_count}",
                  settings.instagramDiscoveryToken())
              .retrieve()
              .body(JsonNode.class);
      JsonNode found = response == null ? null : response.path("business_discovery");
      if (found == null || found.isMissingNode()) return BioReadResult.unavailable();
      Long followers =
          found.hasNonNull("followers_count") ? found.get("followers_count").asLong() : null;
      return BioReadResult.found(found.path("biography").asString(""), followers, null);
    } catch (RestClientResponseException exception) {
      // 개인 계정이거나 없는 사용자면 메타가 오류로 답합니다. 둘을 구분할 수 없어 운영자 확인으로 넘깁니다.
      log.info("인스타그램 Business Discovery 조회 불가: status={}", exception.getStatusCode().value());
      return BioReadResult.unavailable();
    }
  }

  private static String first(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(text);
    return matcher.find() ? matcher.group(1).trim() : "";
  }

  private static String unescape(String value) {
    return value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&");
  }
}
