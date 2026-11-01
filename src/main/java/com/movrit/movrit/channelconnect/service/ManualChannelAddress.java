package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 직접 등록한 채널 주소를 검사하고 표준 형태로 바꿉니다.
 *
 * <ul>
 *   <li>인스타그램: instagram.com/아이디 → https://www.instagram.com/아이디, 표시 이름 @아이디
 *   <li>유튜브: youtube.com/@핸들 또는 youtube.com/channel/UC… → 표시 이름 @핸들 또는 채널 ID
 *   <li>네이버 블로그: blog.naver.com/아이디(모바일 m. 포함) → https://blog.naver.com/아이디
 * </ul>
 *
 * <p>externalId는 'manual:' 접두사를 붙여 로그인 연결에서 받은 SNS 고유 번호와 겹치지 않게 합니다.
 */
public final class ManualChannelAddress {
  private static final Pattern INSTAGRAM =
      Pattern.compile(
          "^(?:https?://)?(?:www\\.|m\\.)?instagram\\.com/([A-Za-z0-9._]{1,30})/?(?:\\?.*)?$");
  private static final Pattern YOUTUBE_HANDLE =
      Pattern.compile(
          "^(?:https?://)?(?:www\\.|m\\.)?youtube\\.com/@([A-Za-z0-9._-]{3,30})/?(?:\\?.*)?$");
  private static final Pattern YOUTUBE_CHANNEL =
      Pattern.compile(
          "^(?:https?://)?(?:www\\.|m\\.)?youtube\\.com/channel/(UC[A-Za-z0-9_-]{22})/?(?:\\?.*)?$");
  private static final Pattern NAVER_BLOG =
      Pattern.compile(
          "^(?:https?://)?(?:m\\.)?blog\\.naver\\.com/([A-Za-z0-9_-]{1,30})/?(?:\\?.*)?$");

  private ManualChannelAddress() {}

  /** 검사한 주소입니다. */
  public record Parsed(String externalId, String accountName, String profileUrl) {}

  /** 서비스에 맞는 주소인지 확인합니다. 형식이 맞지 않으면 IllegalArgumentException(안내 문구 포함)을 던집니다. */
  public static Parsed parse(ChannelProvider provider, String url) {
    String value = url == null ? "" : url.trim();
    switch (provider) {
      case INSTAGRAM -> {
        Matcher m = INSTAGRAM.matcher(value);
        if (m.matches()) {
          String id = m.group(1).toLowerCase();
          return new Parsed("manual:" + id, "@" + id, "https://www.instagram.com/" + id);
        }
        throw new IllegalArgumentException("인스타그램 주소를 확인해 주세요. 예: https://www.instagram.com/아이디");
      }
      case YOUTUBE -> {
        Matcher handle = YOUTUBE_HANDLE.matcher(value);
        if (handle.matches()) {
          String id = handle.group(1);
          return new Parsed(
              "manual:@" + id.toLowerCase(), "@" + id, "https://www.youtube.com/@" + id);
        }
        Matcher channel = YOUTUBE_CHANNEL.matcher(value);
        if (channel.matches()) {
          String id = channel.group(1);
          return new Parsed("manual:" + id, id, "https://www.youtube.com/channel/" + id);
        }
        throw new IllegalArgumentException("유튜브 채널 주소를 확인해 주세요. 예: https://www.youtube.com/@핸들");
      }
      case NAVER_BLOG -> {
        Matcher m = NAVER_BLOG.matcher(value);
        if (m.matches()) {
          String id = m.group(1).toLowerCase();
          return new Parsed("manual:" + id, id, "https://blog.naver.com/" + id);
        }
        throw new IllegalArgumentException("네이버 블로그 주소를 확인해 주세요. 예: https://blog.naver.com/아이디");
      }
      default -> throw new IllegalArgumentException("지원하지 않는 채널입니다.");
    }
  }
}
