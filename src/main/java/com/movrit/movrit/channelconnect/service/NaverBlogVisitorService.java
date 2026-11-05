package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.campaigneligibility.service.CampaignEligibilityPolicyService;
import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import com.movrit.movrit.channelconnect.entity.ChannelVerificationStatus;
import com.movrit.movrit.channelconnect.entity.MemberChannelConnection;
import com.movrit.movrit.channelconnect.repository.MemberChannelConnectionRepository;
import com.movrit.movrit.member.service.MemberService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 회원이 신청 화면에서 '지금 확인'을 누르면 연결한 네이버 블로그의 방문자 수를 읽어 일평균을 기록합니다.
 *
 * <ul>
 *   <li>본인 회원만 자기 블로그를 확인합니다. 블로그는 설정에서 '확인됨'으로 연결되어 있어야 합니다(409).
 *   <li>방문자 위젯이 주는 최근 5일 중 아직 끝나지 않은 오늘(한국 시간)을 빼고 평균을 내어 반올림합니다.
 *   <li>결과는 채널 지표(NAVER_BLOG · DAILY_VISITORS · RECENT_4_DAYS)에 '확인됨'으로 남기고, 이 값은 하루 동안 신청 조건 판정에
 *       쓰입니다.
 *   <li>신청할 때 그 값을 신청 기록에 옮겨 두므로, 신청 뒤에는 위젯을 다시 꺼도 됩니다.
 *   <li>매일 자동으로 다시 읽지 않습니다.
 * </ul>
 */
@Service
public class NaverBlogVisitorService {
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final Pattern BLOG_URL =
      Pattern.compile("^https?://(?:m\\.)?blog\\.naver\\.com/([A-Za-z0-9_-]{1,40})/?$");
  private static final Pattern BLOG_ID = Pattern.compile("^[A-Za-z0-9_-]{1,40}$");

  private final MemberChannelConnectionRepository connectionRepository;
  private final MemberChannelMetricRepository metricRepository;
  private final MemberService memberService;
  private final NaverBlogVisitorReader reader;
  private final Clock clock;

  @Autowired
  public NaverBlogVisitorService(
      MemberChannelConnectionRepository connectionRepository,
      MemberChannelMetricRepository metricRepository,
      MemberService memberService,
      NaverBlogVisitorReader reader) {
    this(connectionRepository, metricRepository, memberService, reader, Clock.system(SEOUL));
  }

  /** 테스트에서 '오늘'을 고정할 때 씁니다. */
  NaverBlogVisitorService(
      MemberChannelConnectionRepository connectionRepository,
      MemberChannelMetricRepository metricRepository,
      MemberService memberService,
      NaverBlogVisitorReader reader,
      Clock clock) {
    this.connectionRepository = connectionRepository;
    this.metricRepository = metricRepository;
    this.memberService = memberService;
    this.reader = reader;
    this.clock = clock;
  }

  /**
   * 내 블로그 방문자 수를 지금 확인합니다.
   *
   * @throws ResponseStatusException 블로그 미연결·미확인(409), 방문자 위젯 꺼짐(409), 네이버 응답 실패(503)
   */
  @Transactional
  public CampaignEligibilityDtos.VisitorCheckResponse checkMine(Long memberId) {
    MemberChannelConnection connection =
        connectionRepository
            .findByMemberIdAndProvider(memberId, ChannelProvider.NAVER_BLOG)
            .filter(found -> found.getVerificationStatus() == ChannelVerificationStatus.VERIFIED)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "먼저 설정에서 네이버 블로그를 소개글 코드로 연결해 주세요."));
    String blogId = blogId(connection);
    NaverBlogVisitorReader.VisitorReadResult result = reader.read(blogId);
    if (result.status() == NaverBlogVisitorReader.Status.WIDGET_OFF) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "블로그 방문자 위젯이 꺼져 있습니다. 블로그 관리 → 꾸미기 설정 → 레이아웃·위젯 설정에서 '방문자' 위젯을 켠 뒤 다시 확인해 주세요.");
    }
    LocalDate today = LocalDate.now(clock);
    List<CampaignEligibilityDtos.DailyVisitors> days =
        result.dailyVisitors().entrySet().stream()
            .filter(entry -> entry.getKey().isBefore(today))
            .sorted(Map.Entry.comparingByKey())
            .map(
                entry ->
                    new CampaignEligibilityDtos.DailyVisitors(entry.getKey(), entry.getValue()))
            .toList();
    if (result.status() != NaverBlogVisitorReader.Status.FOUND || days.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "지금은 블로그 방문자 수를 읽을 수 없습니다. 잠시 후 다시 확인해 주세요.");
    }
    long average =
        Math.round(
            days.stream().mapToLong(CampaignEligibilityDtos.DailyVisitors::visitors).sum()
                / (double) days.size());
    // 확인 시각은 다른 기록과 같은 서버 시각으로 남깁니다(신선도 판정도 같은 기준을 씁니다).
    LocalDateTime measuredAt = LocalDateTime.now();
    record(memberId, average, measuredAt);
    return new CampaignEligibilityDtos.VisitorCheckResponse(average, days.size(), days, measuredAt);
  }

  private void record(Long memberId, long average, LocalDateTime measuredAt) {
    MemberChannelMetric metric =
        metricRepository
            .findByMemberIdAndChannelTypeAndMetricType(
                memberId,
                CampaignEligibilityPolicyService.NAVER_BLOG,
                CampaignEligibilityPolicyService.DAILY_VISITORS)
            .orElseGet(
                () ->
                    MemberChannelMetric.builder()
                        .member(memberService.getMember(memberId))
                        .channelType(CampaignEligibilityPolicyService.NAVER_BLOG)
                        .metricType(CampaignEligibilityPolicyService.DAILY_VISITORS)
                        .value(average)
                        .verificationStatus(MetricVerificationStatus.VERIFIED)
                        .build());
    metric.recordVisitorCheck(average, CampaignEligibilityPolicyService.RECENT_4_DAYS, measuredAt);
    metricRepository.save(metric);
  }

  /** 연결 기록에서 블로그 아이디를 꺼냅니다. 블로그 주소를 먼저 보고, 없으면 등록 아이디에서 'manual:' 접두사를 뗍니다. */
  static String blogId(MemberChannelConnection connection) {
    String url = connection.getProfileUrl();
    if (url != null) {
      Matcher matcher = BLOG_URL.matcher(url.trim());
      if (matcher.matches()) return matcher.group(1);
    }
    String id = connection.getExternalId();
    if (id != null && id.startsWith("manual:")) id = id.substring("manual:".length());
    if (id == null || !BLOG_ID.matcher(id).matches()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "연결한 블로그 주소를 확인할 수 없습니다. 설정에서 블로그를 다시 연결해 주세요.");
    }
    return id;
  }
}
