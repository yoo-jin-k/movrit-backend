package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * 인스타그램·유튜브·네이버와 실제로 통신하는 구현입니다.
 *
 * <ul>
 *   <li>인스타그램: 'Instagram 로그인' 방식(instagram_business_basic 권한). 프로페셔널(비즈니스·크리에이터) 계정만 연결되며 팔로워 수를
 *       받습니다.
 *   <li>유튜브: Google 로그인(youtube.readonly 권한)으로 내 채널의 이름·주소·구독자 수를 받습니다.
 *   <li>네이버 블로그: 네이버 로그인으로 본인을 확인하고, 네이버 아이디(이메일 앞부분)로 블로그 주소를 만든 뒤 공개 RSS로 블로그가 실제로 있는지 확인합니다.
 *       네이버는 이웃 수 API를 제공하지 않아 팔로워 수는 비워 둡니다.
 * </ul>
 *
 * <p>접근 토큰은 계정 정보를 한 번 읽는 데만 쓰고 저장하거나 로그에 남기지 않습니다.
 */
@Slf4j
@Component
public class HttpChannelOAuthGateway implements ChannelOAuthGateway {

  private final ChannelConnectSettings settings;
  private final RestClient http;

  public HttpChannelOAuthGateway(ChannelConnectSettings settings) {
    this.settings = settings;
    this.http = RestClient.create();
  }

  @Override
  public String authorizeUrl(ChannelProvider provider, String state) {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("client_id", settings.clientId(provider));
    query.put("redirect_uri", settings.redirectUri(provider));
    query.put("response_type", "code");
    query.put("state", state);
    String base =
        switch (provider) {
          case INSTAGRAM -> {
            query.put("scope", "instagram_business_basic");
            yield "https://www.instagram.com/oauth/authorize";
          }
          case YOUTUBE -> {
            query.put("scope", "https://www.googleapis.com/auth/youtube.readonly");
            // 여러 Google 계정을 쓰는 경우 연결할 채널의 계정을 고를 수 있게 합니다.
            query.put("prompt", "select_account");
            yield "https://accounts.google.com/o/oauth2/v2/auth";
          }
          case NAVER_BLOG -> "https://nid.naver.com/oauth2.0/authorize";
        };
    return base + "?" + encode(query);
  }

  @Override
  public ConnectedAccount fetchAccount(ChannelProvider provider, String code, String state) {
    try {
      return switch (provider) {
        case INSTAGRAM -> instagram(code);
        case YOUTUBE -> youtube(code);
        case NAVER_BLOG -> naverBlog(code, state);
      };
    } catch (ChannelGatewayException exception) {
      throw exception;
    } catch (RestClientException | IllegalStateException exception) {
      // 응답 본문에 토큰이 섞일 수 있어 예외 종류와 서비스 이름만 기록합니다.
      log.warn(
          "SNS 채널 연결 실패: provider={}, error={}", provider, exception.getClass().getSimpleName());
      throw new ChannelGatewayException(
          provider.label() + "에서 계정 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.", exception);
    }
  }

  // ───────── 인스타그램 ─────────

  private ConnectedAccount instagram(String code) {
    // 인스타그램이 붙여 보내는 '#_' 꼬리는 코드에 포함되지 않습니다.
    String cleanCode = code.endsWith("#_") ? code.substring(0, code.length() - 2) : code;
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", settings.clientId(ChannelProvider.INSTAGRAM));
    form.add("client_secret", settings.clientSecret(ChannelProvider.INSTAGRAM));
    form.add("grant_type", "authorization_code");
    form.add("redirect_uri", settings.redirectUri(ChannelProvider.INSTAGRAM));
    form.add("code", cleanCode);
    JsonNode token = postForm("https://api.instagram.com/oauth/access_token", form);
    String accessToken = text(token, "access_token");

    JsonNode response =
        http.get()
            .uri(
                "https://graph.instagram.com/"
                    + settings.instagramGraphVersion()
                    + "/me?fields=user_id,username,followers_count&access_token={token}",
                accessToken)
            .retrieve()
            .body(JsonNode.class);
    // 문서 예시에 따라 계정 정보가 data 배열 안에 오는 경우와 바로 오는 경우를 모두 받습니다.
    JsonNode me =
        response != null && response.path("data").isArray() && !response.get("data").isEmpty()
            ? response.get("data").get(0)
            : response;
    String username = text(me, "username");
    String userId = me.hasNonNull("user_id") ? me.get("user_id").asString() : text(me, "id");
    Long followers = me.hasNonNull("followers_count") ? me.get("followers_count").asLong() : null;
    return new ConnectedAccount(
        userId, "@" + username, "https://www.instagram.com/" + username, followers);
  }

  // ───────── 유튜브 ─────────

  private ConnectedAccount youtube(String code) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", settings.clientId(ChannelProvider.YOUTUBE));
    form.add("client_secret", settings.clientSecret(ChannelProvider.YOUTUBE));
    form.add("redirect_uri", settings.redirectUri(ChannelProvider.YOUTUBE));
    form.add("grant_type", "authorization_code");
    String accessToken =
        text(postForm("https://oauth2.googleapis.com/token", form), "access_token");

    JsonNode result =
        http.get()
            .uri("https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics&mine=true")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .body(JsonNode.class);
    JsonNode items = result == null ? null : result.get("items");
    if (items == null || items.isEmpty()) {
      throw new ChannelGatewayException("이 Google 계정에는 유튜브 채널이 없습니다. 채널을 만든 계정으로 다시 연결해 주세요.");
    }
    JsonNode channel = items.get(0);
    String channelId = text(channel, "id");
    JsonNode snippet = channel.get("snippet");
    JsonNode statistics = channel.get("statistics");
    String title = snippet == null ? channelId : snippet.path("title").asString(channelId);
    String customUrl = snippet == null ? "" : snippet.path("customUrl").asString("");
    // 구독자 수를 숨긴 채널은 숫자를 받을 수 없으므로 비워 둡니다.
    Long subscribers =
        statistics == null
                || statistics.path("hiddenSubscriberCount").asBoolean(false)
                || !statistics.hasNonNull("subscriberCount")
            ? null
            : Long.parseLong(statistics.get("subscriberCount").asString());
    String profileUrl =
        customUrl.isBlank()
            ? "https://www.youtube.com/channel/" + channelId
            : "https://www.youtube.com/" + customUrl;
    return new ConnectedAccount(channelId, title, profileUrl, subscribers);
  }

  // ───────── 네이버 블로그 ─────────

  private ConnectedAccount naverBlog(String code, String state) {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("grant_type", "authorization_code");
    query.put("client_id", settings.clientId(ChannelProvider.NAVER_BLOG));
    query.put("client_secret", settings.clientSecret(ChannelProvider.NAVER_BLOG));
    query.put("code", code);
    query.put("state", state);
    JsonNode token =
        http.post()
            // 이미 인코딩한 주소이므로 다시 인코딩되지 않도록 URI 객체로 넘깁니다.
            .uri(URI.create("https://nid.naver.com/oauth2.0/token?" + encode(query)))
            .retrieve()
            .body(JsonNode.class);
    String accessToken = text(token, "access_token");

    JsonNode me =
        http.get()
            .uri("https://openapi.naver.com/v1/nid/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .body(JsonNode.class);
    JsonNode profile = me == null ? null : me.get("response");
    if (profile == null || !profile.hasNonNull("id")) {
      throw new IllegalStateException("네이버 회원 정보 응답이 비어 있습니다.");
    }
    String blogId = naverBlogId(profile.path("email").asString(""));
    if (blogId == null) {
      throw new ChannelGatewayException(
          "네이버 아이디를 확인할 수 없습니다. 네이버 아이디(@naver.com) 계정으로 로그인하고 이메일 제공에 동의해 주세요.");
    }
    if (!naverBlogExists(blogId)) {
      throw new ChannelGatewayException("네이버 블로그를 찾을 수 없습니다. 블로그를 개설했는지, 전체 공개인지 확인해 주세요.");
    }
    return new ConnectedAccount(
        profile.get("id").asString(), blogId, "https://blog.naver.com/" + blogId, null);
  }

  /** 네이버 아이디 이메일(아이디@naver.com)의 앞부분이 블로그 아이디입니다. 다른 이메일이면 null입니다. */
  static String naverBlogId(String email) {
    if (email == null) return null;
    String value = email.trim().toLowerCase();
    if (!value.endsWith("@naver.com")) return null;
    String id = value.substring(0, value.length() - "@naver.com".length());
    return id.matches("[a-z0-9_-]{1,30}") ? id : null;
  }

  /** 공개 RSS 주소가 열리면 블로그가 있다고 봅니다. */
  private boolean naverBlogExists(String blogId) {
    try {
      String body =
          http.get()
              .uri("https://rss.blog.naver.com/{id}.xml", blogId)
              .retrieve()
              .body(String.class);
      return body != null && body.contains("<rss");
    } catch (RestClientException exception) {
      return false;
    }
  }

  // ───────── 공통 ─────────

  private JsonNode postForm(String url, MultiValueMap<String, String> form) {
    return http.post()
        .uri(url)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(JsonNode.class);
  }

  /** 꼭 있어야 하는 문자열 값을 꺼냅니다. 없으면 SNS 응답이 예상과 다르다는 뜻이므로 실패로 처리합니다. */
  private static String text(JsonNode node, String field) {
    if (node == null || !node.hasNonNull(field)) {
      throw new IllegalStateException("SNS 응답에 " + field + " 값이 없습니다.");
    }
    return node.get(field).asString();
  }

  private static String encode(Map<String, String> query) {
    return query.entrySet().stream()
        .map(
            entry ->
                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }
}
