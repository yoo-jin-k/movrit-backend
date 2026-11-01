package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SNS 채널 연결에 쓰는 개발자 앱 키와 돌아올 주소(redirect URI)를 모아 둡니다.
 *
 * <ul>
 *   <li>키는 환경 변수로만 받습니다(INSTAGRAM_*, YOUTUBE_*, NAVER_*). 비어 있으면 그 서비스는 '준비 중'으로 표시하고 연결을 시작하지
 *       않습니다.
 *   <li>유튜브는 따로 키를 주지 않으면 Google 로그인 키(GOOGLE_*)를 함께 씁니다. 이때 Google 콘솔에서 YouTube Data API 사용과 아래
 *       돌아올 주소 등록이 필요합니다.
 *   <li>돌아올 주소는 '화면 주소 + /channel-connect/callback/{서비스}'입니다. 각 개발자 앱에 이 주소를 똑같이 등록해야 합니다.
 * </ul>
 */
@Component
public class ChannelConnectSettings {

  @Value("${movrit.channel-connect.redirect-base:http://localhost:5174}")
  private String redirectBase;

  @Value("${movrit.channel-connect.instagram.client-id:}")
  private String instagramClientId;

  @Value("${movrit.channel-connect.instagram.client-secret:}")
  private String instagramClientSecret;

  @Value("${movrit.channel-connect.youtube.client-id:}")
  private String youtubeClientId;

  @Value("${movrit.channel-connect.youtube.client-secret:}")
  private String youtubeClientSecret;

  // 유튜브 키가 비어 있으면 Google 로그인 키를 대신 씁니다(같은 Google 클라이언트에 YouTube 권한을 추가하는 방식).
  @Value("${google.client-id:}")
  private String googleClientId;

  @Value("${google.client-secret:}")
  private String googleClientSecret;

  @Value("${movrit.channel-connect.naver.client-id:}")
  private String naverClientId;

  @Value("${movrit.channel-connect.naver.client-secret:}")
  private String naverClientSecret;

  /** 인스타그램 Graph API 버전입니다. 메타가 버전을 올리면 환경 변수로 바꿉니다. */
  @Value("${movrit.channel-connect.instagram.graph-version:v26.0}")
  private String instagramGraphVersion;

  public String clientId(ChannelProvider provider) {
    return switch (provider) {
      case INSTAGRAM -> instagramClientId;
      case YOUTUBE -> notBlank(youtubeClientId) ? youtubeClientId : googleClientId;
      case NAVER_BLOG -> naverClientId;
    };
  }

  public String clientSecret(ChannelProvider provider) {
    return switch (provider) {
      case INSTAGRAM -> instagramClientSecret;
      case YOUTUBE -> notBlank(youtubeClientId) ? youtubeClientSecret : googleClientSecret;
      case NAVER_BLOG -> naverClientSecret;
    };
  }

  /** 키와 비밀값이 모두 있어야 연결을 시작할 수 있습니다. */
  public boolean configured(ChannelProvider provider) {
    return notBlank(clientId(provider)) && notBlank(clientSecret(provider));
  }

  /** SNS 로그인 후 돌아올 화면 주소입니다. 개발자 앱에 등록한 값과 글자까지 같아야 합니다. */
  public String redirectUri(ChannelProvider provider) {
    String base =
        redirectBase.endsWith("/")
            ? redirectBase.substring(0, redirectBase.length() - 1)
            : redirectBase;
    return base + "/channel-connect/callback/" + provider.slug();
  }

  public String instagramGraphVersion() {
    return instagramGraphVersion;
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}
