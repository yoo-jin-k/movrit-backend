package com.movrit.movrit.channelconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.channelconnect.dto.ChannelConnectDtos;
import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import com.movrit.movrit.channelconnect.entity.MemberChannelConnection;
import com.movrit.movrit.channelconnect.repository.MemberChannelConnectionRepository;
import com.movrit.movrit.channelconnect.service.ChannelOAuthGateway.ConnectedAccount;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** SNS 채널 연결의 시작·완료·해제 규칙을 SNS 통신 없이 확인합니다. */
class ChannelConnectionServiceTest {
  private final MemberChannelConnectionRepository repository =
      mock(MemberChannelConnectionRepository.class);
  private final MemberChannelMetricRepository metrics = mock(MemberChannelMetricRepository.class);
  private final MemberService members = mock(MemberService.class);
  private final ChannelConnectSettings settings = mock(ChannelConnectSettings.class);
  private final ChannelOAuthStateStore states = new ChannelOAuthStateStore();
  private final ChannelOAuthGateway gateway = mock(ChannelOAuthGateway.class);
  private final ChannelConnectionService service =
      new ChannelConnectionService(
          repository, metrics, members, settings, states, gateway, mock(AuditLogService.class));

  private final Member member = Member.builder().id(7L).name("참여자").build();

  @Test
  void providerWithoutKeysCannotStart() {
    when(settings.configured(ChannelProvider.INSTAGRAM)).thenReturn(false);

    assertThatThrownBy(() -> service.start(7L, "instagram"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
  }

  @Test
  void completeSavesAccountAndRecordsVerifiedSubscribers() {
    when(settings.configured(ChannelProvider.YOUTUBE)).thenReturn(true);
    when(gateway.authorizeUrl(eq(ChannelProvider.YOUTUBE), anyString()))
        .thenReturn("https://login");
    when(gateway.fetchAccount(eq(ChannelProvider.YOUTUBE), eq("code-1"), anyString()))
        .thenReturn(
            new ConnectedAccount("UC1", "무브릿 채널", "https://www.youtube.com/@movrit", 1200L));
    when(repository.findByProviderAndExternalId(ChannelProvider.YOUTUBE, "UC1"))
        .thenReturn(Optional.empty());
    when(repository.findByMemberIdAndProvider(7L, ChannelProvider.YOUTUBE))
        .thenReturn(Optional.empty());
    when(metrics.findByMemberIdAndChannelTypeAndMetricType(7L, "YOUTUBE", "SUBSCRIBER_COUNT"))
        .thenReturn(Optional.empty());
    when(members.getMember(7L)).thenReturn(member);

    service.start(7L, "youtube");
    ArgumentCaptor<String> state = ArgumentCaptor.forClass(String.class);
    verify(gateway).authorizeUrl(eq(ChannelProvider.YOUTUBE), state.capture());

    ChannelConnectDtos.ProviderStatus result =
        service.complete(
            7L, "youtube", new ChannelConnectDtos.CompleteRequest("code-1", state.getValue()));

    assertThat(result.connection().accountName()).isEqualTo("무브릿 채널");
    assertThat(result.connection().followerCount()).isEqualTo(1200L);
    ArgumentCaptor<MemberChannelMetric> metric = ArgumentCaptor.forClass(MemberChannelMetric.class);
    verify(metrics).save(metric.capture());
    assertThat(metric.getValue().getValue()).isEqualTo(1200L);
    assertThat(metric.getValue().getVerificationStatus())
        .isEqualTo(MetricVerificationStatus.VERIFIED);
  }

  @Test
  void wrongStateIsRejectedBeforeCallingSns() {
    when(settings.configured(ChannelProvider.NAVER_BLOG)).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.complete(
                    7L, "naver-blog", new ChannelConnectDtos.CompleteRequest("code", "forged")))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    verify(gateway, never()).fetchAccount(any(), anyString(), anyString());
  }

  @Test
  void accountLinkedToAnotherMemberIsRejected() {
    when(settings.configured(ChannelProvider.INSTAGRAM)).thenReturn(true);
    when(gateway.fetchAccount(eq(ChannelProvider.INSTAGRAM), anyString(), anyString()))
        .thenReturn(
            new ConnectedAccount("IG1", "@movrit", "https://www.instagram.com/movrit", 10L));
    MemberChannelConnection other =
        MemberChannelConnection.builder()
            .member(Member.builder().id(99L).build())
            .provider(ChannelProvider.INSTAGRAM)
            .externalId("IG1")
            .build();
    when(repository.findByProviderAndExternalId(ChannelProvider.INSTAGRAM, "IG1"))
        .thenReturn(Optional.of(other));
    String state = states.issue(7L, ChannelProvider.INSTAGRAM);

    assertThatThrownBy(
            () ->
                service.complete(
                    7L, "instagram", new ChannelConnectDtos.CompleteRequest("code", state)))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void statusListKeepsProviderOrder() {
    when(repository.findAllByMemberId(7L)).thenReturn(List.of());
    when(settings.configured(any())).thenReturn(false);

    assertThat(service.findMine(7L))
        .extracting(ChannelConnectDtos.ProviderStatus::provider)
        .containsExactly("instagram", "youtube", "naver-blog");
  }
}
