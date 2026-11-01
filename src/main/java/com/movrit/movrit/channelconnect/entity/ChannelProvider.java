package com.movrit.movrit.channelconnect.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * 계정 로그인으로 연결할 수 있는 SNS 서비스입니다.
 *
 * <p>slug는 주소에 쓰는 이름(예: /channel-connect/callback/naver-blog)이고, channelType·metricType은 기존 채널 지표
 * (member_channel_metric)에 연결 결과를 '확인됨'으로 기록할 때 쓰는 이름입니다. 네이버 블로그는 이웃 수를 주는 공개 API가 없어 지표를 기록하지
 * 않습니다.
 */
public enum ChannelProvider {
  INSTAGRAM("instagram", "인스타그램", "INSTAGRAM", "FOLLOWER_COUNT"),
  YOUTUBE("youtube", "유튜브", "YOUTUBE", "SUBSCRIBER_COUNT"),
  NAVER_BLOG("naver-blog", "네이버 블로그", "NAVER_BLOG", null);

  private final String slug;
  private final String label;
  private final String channelType;
  private final String metricType;

  ChannelProvider(String slug, String label, String channelType, String metricType) {
    this.slug = slug;
    this.label = label;
    this.channelType = channelType;
    this.metricType = metricType;
  }

  public String slug() {
    return slug;
  }

  public String label() {
    return label;
  }

  public String channelType() {
    return channelType;
  }

  /** 연결 결과로 기록할 지표 이름입니다. 기록할 지표가 없으면 null입니다. */
  public String metricType() {
    return metricType;
  }

  /** 주소의 이름(instagram·youtube·naver-blog)으로 서비스를 찾습니다. */
  public static Optional<ChannelProvider> fromSlug(String slug) {
    return Arrays.stream(values()).filter(value -> value.slug.equals(slug)).findFirst();
  }
}
