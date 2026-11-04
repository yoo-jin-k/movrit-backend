package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.channelconnect.dto.ChannelConnectDtos;
import com.movrit.movrit.channelconnect.entity.ChannelConnectionMethod;
import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import com.movrit.movrit.channelconnect.entity.ChannelVerificationStatus;
import com.movrit.movrit.channelconnect.entity.MemberChannelConnection;
import com.movrit.movrit.channelconnect.repository.MemberChannelConnectionRepository;
import com.movrit.movrit.channelconnect.service.ChannelOAuthGateway.ChannelGatewayException;
import com.movrit.movrit.channelconnect.service.ChannelOAuthGateway.ConnectedAccount;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 회원의 인스타그램·유튜브·네이버 블로그 계정 연결을 처리합니다.
 *
 * <p>흐름: ① 연결 시작(state 발급 후 SNS 로그인 주소 안내) → ② 회원이 SNS에서 로그인·동의 → ③ 화면이 돌아온 인가 코드를 넘기면 state를 확인하고
 * 계정 정보를 받아 저장합니다.
 *
 * <p>로그인 연결이 안 되는 계정(예: 개인 인스타그램)은 '직접 등록'으로 주소·팔로워 수·프로필 캡처를 올리고, 운영자가 확인하거나 반려합니다.
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
  private final ChannelEvidenceStorage evidenceStorage;
  private final NotificationService notificationService;

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
    // 직접 등록했던 채널을 로그인으로 다시 연결하면 예전 캡처는 더 필요 없으므로 지웁니다.
    String oldEvidence = connection.getEvidenceKey();
    connection.refresh(
        account.externalId(), account.accountName(), account.profileUrl(), account.followerCount());
    repository.save(connection);
    evidenceStorage.delete(oldEvidence);
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
              evidenceStorage.delete(connection.getEvidenceKey());
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

  // ───────── 직접 등록(로그인 연결이 안 되는 계정) ─────────

  /**
   * 채널을 직접 등록합니다. 주소 형식을 검사하고 프로필 캡처를 저장한 뒤 '확인 대기'로 둡니다.
   *
   * <ul>
   *   <li>인스타그램·유튜브는 팔로워·구독자 수가 꼭 있어야 합니다. 네이버 블로그 이웃 수는 선택입니다.
   *   <li>이미 로그인으로 연결된 서비스는 직접 등록으로 덮어쓰지 않습니다(409). 해제 후 등록해야 합니다.
   *   <li>같은 주소를 다른 회원이 이미 등록했다면 409로 막습니다.
   *   <li>등록한 수치는 채널 지표에 '미확인'으로 기록되고, 운영자가 확인하면 '확인됨'이 됩니다.
   * </ul>
   */
  @Transactional
  public ChannelConnectDtos.ProviderStatus registerManually(
      Long memberId, String slug, String profileUrl, Long followerCount, MultipartFile evidence) {
    ChannelProvider provider = provider(slug);
    ManualChannelAddress.Parsed address;
    try {
      address = ManualChannelAddress.parse(provider, profileUrl);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
    if (provider.metricType() != null && followerCount == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          provider == ChannelProvider.YOUTUBE ? "구독자 수를 입력해 주세요." : "팔로워 수를 입력해 주세요.");
    }
    if (followerCount != null && (followerCount < 0 || followerCount > 1_000_000_000L)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "팔로워 수를 다시 확인해 주세요.");
    }
    MemberChannelConnection existing =
        repository.findByMemberIdAndProvider(memberId, provider).orElse(null);
    if (existing != null && existing.getConnectionMethod() == ChannelConnectionMethod.OAUTH) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "이미 로그인으로 연결된 " + provider.label() + " 계정이 있습니다. 해제한 뒤 직접 등록해 주세요.");
    }
    repository
        .findByProviderAndExternalId(provider, address.externalId())
        .filter(other -> !other.getMember().getId().equals(memberId))
        .ifPresent(
            other -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "이미 다른 MOVRIT 회원이 등록한 " + provider.label() + " 주소입니다.");
            });

    String evidenceKey = evidenceStorage.store(evidence);
    MemberChannelConnection connection =
        existing != null
            ? existing
            : MemberChannelConnection.builder()
                .member(memberService.getMember(memberId))
                .provider(provider)
                .build();
    String oldEvidence = connection.getEvidenceKey();
    connection.registerManually(
        address.externalId(),
        address.accountName(),
        address.profileUrl(),
        followerCount,
        evidenceKey);
    try {
      repository.save(connection);
    } catch (RuntimeException exception) {
      evidenceStorage.delete(evidenceKey);
      throw exception;
    }
    evidenceStorage.delete(oldEvidence);
    recordMemberMetric(memberId, provider, followerCount);
    auditLogService.record(
        memberId,
        "MEMBER_CHANNEL_MANUAL_REGISTERED",
        "MEMBER_CHANNEL_CONNECTION",
        connection.getId(),
        null,
        "{\"provider\":\"" + provider + "\",\"status\":\"PENDING\"}",
        null);
    return new ChannelConnectDtos.ProviderStatus(
        provider.slug(),
        provider.label(),
        settings.configured(provider),
        ChannelConnectDtos.Connection.from(connection));
  }

  /** 업체가 참여자 상세에서 볼 연결 채널입니다. 반려된 채널은 빼고 서비스 순서대로 돌려줍니다. */
  @Transactional(readOnly = true)
  public List<ChannelConnectDtos.LinkedChannel> linkedChannels(Long memberId) {
    return repository.findAllByMemberId(memberId).stream()
        .filter(value -> value.getVerificationStatus() != ChannelVerificationStatus.REJECTED)
        .sorted(java.util.Comparator.comparing(MemberChannelConnection::getProvider))
        .map(ChannelConnectDtos.LinkedChannel::from)
        .toList();
  }

  // ───────── 운영자 확인 ─────────

  /**
   * 운영자 '채널 확인' 목록입니다. 직접 등록한 채널과, 자동 확인이 안 되어 넘어온 소개글 코드 등록을 보여주고 확인 대기가 먼저 나옵니다. 소개글 코드로 자동 확인된
   * 채널은 운영자가 볼 일이 없어 뺍니다.
   */
  @Transactional(readOnly = true)
  public List<ChannelConnectDtos.ReviewItem> reviewList(Long adminMemberId) {
    memberService.getAdmin(adminMemberId);
    return repository
        .findAllByConnectionMethodInOrderByConnectedAtAsc(
            List.of(ChannelConnectionMethod.MANUAL, ChannelConnectionMethod.BIO_CODE))
        .stream()
        .filter(
            value ->
                value.getConnectionMethod() == ChannelConnectionMethod.MANUAL
                    || value.getVerificationStatus() != ChannelVerificationStatus.VERIFIED
                    || value.getReviewedByMemberId() != null)
        .sorted(
            java.util.Comparator.comparing(
                (MemberChannelConnection value) ->
                    value.getVerificationStatus() != ChannelVerificationStatus.PENDING))
        .map(ChannelConnectDtos.ReviewItem::from)
        .toList();
  }

  /** 운영자가 캡처를 볼 수 있도록 파일 위치를 돌려줍니다. */
  @Transactional(readOnly = true)
  public java.nio.file.Path evidence(Long adminMemberId, Long connectionId) {
    memberService.getAdmin(adminMemberId);
    MemberChannelConnection connection = getConnection(connectionId);
    if (connection.getEvidenceKey() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "캡처 이미지가 없습니다.");
    }
    return evidenceStorage.load(connection.getEvidenceKey());
  }

  /** 운영자가 직접 등록한 채널을 확인합니다. 수치는 '확인됨'이 되고 회원에게 알림이 갑니다. */
  @Transactional
  public ChannelConnectDtos.ReviewItem approve(Long adminMemberId, Long connectionId) {
    memberService.getAdmin(adminMemberId);
    MemberChannelConnection connection = getConnection(connectionId);
    review(() -> connection.approve(adminMemberId));
    Long memberId = connection.getMember().getId();
    ChannelProvider provider = connection.getProvider();
    recordVerifiedMetric(memberId, provider, connection.getFollowerCount());
    notificationService.send(
        memberId,
        "CHANNEL_VERIFICATION",
        provider.label() + " 채널 확인이 완료되었습니다.",
        connection.getAccountName() + " 채널이 확인되었습니다.",
        "MEMBER_CHANNEL_CONNECTION",
        connection.getId());
    auditLogService.record(
        adminMemberId,
        "MEMBER_CHANNEL_VERIFIED",
        "MEMBER_CHANNEL_CONNECTION",
        connection.getId(),
        "{\"status\":\"PENDING\"}",
        "{\"status\":\"VERIFIED\"}",
        null);
    return ChannelConnectDtos.ReviewItem.from(connection);
  }

  /** 운영자가 직접 등록한 채널을 사유와 함께 반려합니다. 수치는 '반려'가 되고 회원에게 사유가 전달됩니다. */
  @Transactional
  public ChannelConnectDtos.ReviewItem reject(
      Long adminMemberId, Long connectionId, String reason) {
    memberService.getAdmin(adminMemberId);
    MemberChannelConnection connection = getConnection(connectionId);
    String note = reason.trim();
    review(() -> connection.reject(adminMemberId, note));
    Long memberId = connection.getMember().getId();
    ChannelProvider provider = connection.getProvider();
    if (provider.metricType() != null) {
      metricRepository
          .findByMemberIdAndChannelTypeAndMetricType(
              memberId, provider.channelType(), provider.metricType())
          .ifPresent(MemberChannelMetric::markRejected);
    }
    notificationService.send(
        memberId,
        "CHANNEL_VERIFICATION",
        provider.label() + " 채널 확인이 반려되었습니다.",
        "사유: " + note,
        "MEMBER_CHANNEL_CONNECTION",
        connection.getId());
    auditLogService.record(
        adminMemberId,
        "MEMBER_CHANNEL_REJECTED",
        "MEMBER_CHANNEL_CONNECTION",
        connection.getId(),
        "{\"status\":\"PENDING\"}",
        "{\"status\":\"REJECTED\"}",
        note);
    return ChannelConnectDtos.ReviewItem.from(connection);
  }

  private MemberChannelConnection getConnection(Long connectionId) {
    return repository
        .findById(connectionId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채널 등록 내역을 찾을 수 없습니다."));
  }

  /** 상태 규칙 위반(이미 처리됨 등)은 409로 바꿉니다. */
  private static void review(Runnable action) {
    try {
      action.run();
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 확인 요청입니다. 최신 목록을 확인해 주세요.");
    }
  }

  /** 직접 등록한 수치를 채널 지표에 '미확인'으로 기록합니다. 수치가 없거나 지표가 없는 서비스면 건너뜁니다. */
  void recordMemberMetric(Long memberId, ChannelProvider provider, Long value) {
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
                        .verificationStatus(MetricVerificationStatus.UNVERIFIED)
                        .build());
    if (metric.getId() != null) metric.updateByMember(value, metric.getMeasurementPeriod(), now);
    metricRepository.save(metric);
  }

  /** 계정 연결로 받은 팔로워·구독자 수를 채널 지표에 '확인됨'으로 기록합니다. 수치를 받지 못했으면 기록하지 않습니다. */
  void recordVerifiedMetric(Long memberId, ChannelProvider provider, Long value) {
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
          provider.label() + " 로그인 연결은 아직 열리지 않았습니다. 직접 등록을 이용해 주세요.");
    }
  }
}
