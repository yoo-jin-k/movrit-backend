package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.channelconnect.dto.ChannelConnectDtos;
import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import com.movrit.movrit.channelconnect.entity.MemberChannelConnection;
import com.movrit.movrit.channelconnect.repository.MemberChannelConnectionRepository;
import com.movrit.movrit.channelconnect.service.ChannelOAuthGateway.ChannelGatewayException;
import com.movrit.movrit.channelconnect.service.ChannelOAuthGateway.ConnectedAccount;
import com.movrit.movrit.member.service.MemberService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 회원의 인스타그램·유튜브·네이버 블로그 계정 연결을 처리합니다.
 *
 * <p>흐름: ① 연결 시작(state 발급 후 SNS 로그인 주소 안내) → ② 회원이 SNS에서 로그인·동의 → ③ 화면이 돌아온 인가 코드를 넘기면 state를 확인하고
 * 계정 정보를 받아 저장합니다.
 *
 * <ul>
 *   <li>같은 SNS 계정을 다른 회원이 이미 연결했다면 409로 막습니다.
 *   <li>팔로워·구독자 수를 받으면 기존 채널 지표에 '확인됨(VERIFIED)'으로 기록해 캠페인 참여 조건 판정에 쓰입니다.
 *   <li>연결을 해제하면 연결 기록을 지우고, 그 연결로 확인했던 지표는 '미확인'으로 되돌립니다(수치는 남깁니다).
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ChannelConnectionService {
  private final MemberChannelConnectionRepository repository;
  private final MemberChannelMetricRepository metricRepository;
  private final MemberService memberService;
  private final ChannelConnectSettings settings;
  private final ChannelOAuthStateStore stateStore;
  private final ChannelOAuthGateway gateway;
  private final AuditLogService auditLogService;

  /** 세 서비스의 연결 상태를 정해진 순서(인스타그램·유튜브·네이버 블로그)로 돌려줍니다. */
  @Transactional(readOnly = true)
  public List<ChannelConnectDtos.ProviderStatus> findMine(Long memberId) {
    Map<ChannelProvider, MemberChannelConnection> connected =
        repository.findAllByMemberId(memberId).stream()
            .collect(Collectors.toMap(MemberChannelConnection::getProvider, Function.identity()));
    return Arrays.stream(ChannelProvider.values())
        .map(
            provider -> {
              MemberChannelConnection connection = connected.get(provider);
              return new ChannelConnectDtos.ProviderStatus(
                  provider.slug(),
                  provider.label(),
                  settings.configured(provider),
                  connection == null ? null : ChannelConnectDtos.Connection.from(connection));
            })
        .toList();
  }

  /**
   * 연결을 시작합니다. 한 번만 쓰는 state를 발급하고 SNS 로그인 주소를 돌려줍니다.
   *
   * @throws ResponseStatusException 없는 서비스면 404, 서버에 연동 키가 없으면 503
   */
  public ChannelConnectDtos.StartResponse start(Long memberId, String slug) {
    ChannelProvider provider = provider(slug);
    requireConfigured(provider);
    String state = stateStore.issue(memberId, provider);
    return new ChannelConnectDtos.StartResponse(gateway.authorizeUrl(provider, state));
  }

  /**
   * SNS 로그인 후 돌아온 인가 코드로 계정을 연결합니다. 이미 연결된 서비스면 최신 정보로 바꿉니다.
   *
   * @throws ResponseStatusException state가 맞지 않거나 만료되면 400, 다른 회원이 연결한 계정이면 409, SNS 통신 실패면 502
   */
  @Transactional
  public ChannelConnectDtos.ProviderStatus complete(
      Long memberId, String slug, ChannelConnectDtos.CompleteRequest request) {
    ChannelProvider provider = provider(slug);
    requireConfigured(provider);
    if (!stateStore.consume(request.state(), memberId, provider)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "연결 요청이 만료되었거나 올바르지 않습니다. 처음부터 다시 연결해 주세요.");
    }
    ConnectedAccount account;
    try {
      account = gateway.fetchAccount(provider, request.code(), request.state());
    } catch (ChannelGatewayException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }
    repository
        .findByProviderAndExternalId(provider, account.externalId())
        .filter(other -> !other.getMember().getId().equals(memberId))
        .ifPresent(
            other -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "이미 다른 MOVRIT 회원에게 연결된 " + provider.label() + " 계정입니다.");
            });

    MemberChannelConnection connection =
        repository
            .findByMemberIdAndProvider(memberId, provider)
            .orElseGet(
                () ->
                    MemberChannelConnection.builder()
                        .member(memberService.getMember(memberId))
                        .provider(provider)
                        .build());
    connection.refresh(
        account.externalId(), account.accountName(), account.profileUrl(), account.followerCount());
    repository.save(connection);
    recordVerifiedMetric(memberId, provider, account.followerCount());
    auditLogService.record(
        memberId,
        "MEMBER_CHANNEL_CONNECTED",
        "MEMBER_CHANNEL_CONNECTION",
        connection.getId(),
        null,
        "{\"provider\":\"" + provider + "\"}",
        null);
    return new ChannelConnectDtos.ProviderStatus(
        provider.slug(), provider.label(), true, ChannelConnectDtos.Connection.from(connection));
  }

  /** 연결을 해제합니다. 연결 기록을 지우고 그 연결로 확인한 지표는 '미확인'으로 되돌립니다. 연결이 없으면 아무것도 하지 않습니다. */
  @Transactional
  public void disconnect(Long memberId, String slug) {
    ChannelProvider provider = provider(slug);
    repository
        .findByMemberIdAndProvider(memberId, provider)
        .ifPresent(
            connection -> {
              repository.delete(connection);
              if (provider.metricType() != null) {
                metricRepository
                    .findByMemberIdAndChannelTypeAndMetricType(
                        memberId, provider.channelType(), provider.metricType())
                    .ifPresent(MemberChannelMetric::markUnverified);
              }
              auditLogService.record(
                  memberId,
                  "MEMBER_CHANNEL_DISCONNECTED",
                  "MEMBER_CHANNEL_CONNECTION",
                  connection.getId(),
                  "{\"provider\":\"" + provider + "\"}",
                  null,
                  null);
            });
  }

  /** 계정 연결로 받은 팔로워·구독자 수를 채널 지표에 '확인됨'으로 기록합니다. 수치를 받지 못했으면 기록하지 않습니다. */
  private void recordVerifiedMetric(Long memberId, ChannelProvider provider, Long value) {
    if (provider.metricType() == null || value == null) return;
    LocalDateTime now = LocalDateTime.now();
    MemberChannelMetric metric =
        metricRepository
            .findByMemberIdAndChannelTypeAndMetricType(
                memberId, provider.channelType(), provider.metricType())
            .orElseGet(
                () ->
                    MemberChannelMetric.builder()
                        .member(memberService.getMember(memberId))
                        .channelType(provider.channelType())
                        .metricType(provider.metricType())
                        .value(value)
                        .measuredAt(now)
                        .verificationStatus(MetricVerificationStatus.VERIFIED)
                        .build());
    metric.verifyFromConnection(value, now);
    metricRepository.save(metric);
  }

  private ChannelProvider provider(String slug) {
    return ChannelProvider.fromSlug(slug)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지원하지 않는 채널입니다."));
  }

  private void requireConfigured(ChannelProvider provider) {
    if (!settings.configured(provider)) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          provider.label() + " 연결은 아직 준비 중입니다. 운영자가 연동 키를 등록하면 사용할 수 있습니다.");
    }
  }
}
