package com.movrit.movrit.channelconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import com.movrit.movrit.channelconnect.entity.ChannelVerificationStatus;
import com.movrit.movrit.channelconnect.entity.MemberChannelConnection;
import com.movrit.movrit.channelconnect.repository.MemberChannelConnectionRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

/** 블로그 방문자 수 '지금 확인'의 평균 계산·연결 확인·위젯 꺼짐 처리를 네이버 통신 없이 확인합니다. */
class NaverBlogVisitorServiceTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

  private final MemberChannelConnectionRepository connections =
      mock(MemberChannelConnectionRepository.class);
  private final MemberChannelMetricRepository metrics = mock(MemberChannelMetricRepository.class);
  private final MemberService members = mock(MemberService.class);
  private final NaverBlogVisitorReader reader = mock(NaverBlogVisitorReader.class);
  private final NaverBlogVisitorService service =
      new NaverBlogVisitorService(
          connections,
          metrics,
          members,
          reader,
          Clock.fixed(
              ZonedDateTime.of(TODAY.atTime(15, 0), ZoneId.of("Asia/Seoul")).toInstant(),
              ZoneId.of("Asia/Seoul")));

  @Test
  void averageExcludesTodayAndIsRecordedAsVerified() {
    connected(ChannelVerificationStatus.VERIFIED);
    when(reader.read("dbwls_0811"))
        .thenReturn(
            NaverBlogVisitorReader.VisitorReadResult.found(
                Map.of(
                    TODAY.minusDays(4),
                    10L,
                    TODAY.minusDays(3),
                    20L,
                    TODAY.minusDays(2),
                    30L,
                    TODAY.minusDays(1),
                    41L,
                    TODAY,
                    999L)));
    when(metrics.findByMemberIdAndChannelTypeAndMetricType(7L, "NAVER_BLOG", "DAILY_VISITORS"))
        .thenReturn(Optional.empty());
    when(members.getMember(7L)).thenReturn(Member.builder().id(7L).build());

    var result = service.checkMine(7L);

    // (10 + 20 + 30 + 41) / 4 = 25.25 → 25. 아직 끝나지 않은 오늘(999)은 넣지 않습니다.
    assertThat(result.averageVisitors()).isEqualTo(25L);
    assertThat(result.days()).isEqualTo(4);
    assertThat(result.dailyVisitors()).extracting("date").doesNotContain(TODAY);
    ArgumentCaptor<MemberChannelMetric> saved = ArgumentCaptor.forClass(MemberChannelMetric.class);
    verify(metrics).save(saved.capture());
    assertThat(saved.getValue().getValue()).isEqualTo(25L);
    assertThat(saved.getValue().getMeasurementPeriod()).isEqualTo("RECENT_4_DAYS");
    assertThat(saved.getValue().getVerificationStatus())
        .isEqualTo(MetricVerificationStatus.VERIFIED);
    assertThat(saved.getValue().getMeasuredAt()).isNotNull();
  }

  @Test
  void blogMustBeConnectedAndVerified() {
    when(connections.findByMemberIdAndProvider(7L, ChannelProvider.NAVER_BLOG))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.checkMine(7L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("네이버 블로그를 소개글 코드로 연결");

    connected(ChannelVerificationStatus.PENDING);
    assertThatThrownBy(() -> service.checkMine(7L)).isInstanceOf(ResponseStatusException.class);
    verify(reader, never()).read(any());
  }

  @Test
  void widgetOffAsksMemberToTurnOnVisitorWidget() {
    connected(ChannelVerificationStatus.VERIFIED);
    when(reader.read("dbwls_0811"))
        .thenReturn(NaverBlogVisitorReader.VisitorReadResult.widgetOff());

    assertThatThrownBy(() -> service.checkMine(7L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("'방문자' 위젯");
    verify(metrics, never()).save(any());
  }

  @Test
  void readerFailureDoesNotRecordAnything() {
    connected(ChannelVerificationStatus.VERIFIED);
    when(reader.read("dbwls_0811"))
        .thenReturn(NaverBlogVisitorReader.VisitorReadResult.unavailable());

    assertThatThrownBy(() -> service.checkMine(7L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("잠시 후 다시");
    verify(metrics, never()).save(any());
  }

  @Test
  void blogIdComesFromProfileUrlOrManualExternalId() {
    MemberChannelConnection byUrl = mock(MemberChannelConnection.class);
    when(byUrl.getProfileUrl()).thenReturn("https://m.blog.naver.com/some_blog");
    assertThat(NaverBlogVisitorService.blogId(byUrl)).isEqualTo("some_blog");

    MemberChannelConnection byId = mock(MemberChannelConnection.class);
    when(byId.getExternalId()).thenReturn("manual:other-blog");
    assertThat(NaverBlogVisitorService.blogId(byId)).isEqualTo("other-blog");

    MemberChannelConnection broken = mock(MemberChannelConnection.class);
    when(broken.getExternalId()).thenReturn("manual:../evil?x=1");
    assertThatThrownBy(() -> NaverBlogVisitorService.blogId(broken))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void widgetResponseIsParsedByDate() {
    var result =
        HttpNaverBlogVisitorReader.parse(
            "<visitorcnts><visitorcnt id=\"20261101\" cnt=\"1\"/>"
                + "<visitorcnt id=\"20261102\" cnt=\"0\"/></visitorcnts>");
    assertThat(result.status()).isEqualTo(NaverBlogVisitorReader.Status.FOUND);
    assertThat(result.dailyVisitors()).containsEntry(LocalDate.of(2026, 11, 1), 1L).hasSize(2);
    assertThat(HttpNaverBlogVisitorReader.parse("").status())
        .isEqualTo(NaverBlogVisitorReader.Status.WIDGET_OFF);
    assertThat(HttpNaverBlogVisitorReader.parse("<html>").status())
        .isEqualTo(NaverBlogVisitorReader.Status.UNAVAILABLE);
  }

  private void connected(ChannelVerificationStatus status) {
    MemberChannelConnection connection = mock(MemberChannelConnection.class);
    when(connection.getVerificationStatus()).thenReturn(status);
    when(connection.getProfileUrl()).thenReturn("https://blog.naver.com/dbwls_0811");
    when(connection.getExternalId()).thenReturn("manual:dbwls_0811");
    when(connections.findByMemberIdAndProvider(7L, ChannelProvider.NAVER_BLOG))
        .thenReturn(Optional.of(connection));
  }
}
