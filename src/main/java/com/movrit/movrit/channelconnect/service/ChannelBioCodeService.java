package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.channelconnect.dto.ChannelConnectDtos;
import com.movrit.movrit.channelconnect.entity.ChannelBioCode;
import com.movrit.movrit.channelconnect.entity.ChannelConnectionMethod;
import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import com.movrit.movrit.channelconnect.entity.MemberChannelConnection;
import com.movrit.movrit.channelconnect.repository.ChannelBioCodeRepository;
import com.movrit.movrit.channelconnect.repository.MemberChannelConnectionRepository;
import com.movrit.movrit.channelconnect.service.ChannelBioReader.BioReadResult;
import com.movrit.movrit.member.service.MemberService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * SNS 소개글 인증 코드로 채널을 등록합니다(인스타그램·네이버 블로그).
 *
 * <p>흐름: ① 회원이 코드를 받음(24시간 유효) → ② 소개글에 코드를 붙여 넣고 채널 주소를 입력 → ③ 서버가 공개 정보에서 코드를 찾으면 바로 '확인됨'.
 *
 * <ul>
 *   <li>네이버 블로그는 공개 RSS로 항상 자동 확인합니다. 인스타그램은 Business Discovery 키가 있고 비즈니스·크리에이터 계정일 때 자동 확인하며 팔로워
 *       수도 '확인됨'으로 남깁니다.
 *   <li>인스타그램을 자동으로 확인할 수 없으면(키 없음·개인 계정·통신 실패) 팔로워 수를 받아 '확인 대기'로 두고, 운영자가 소개글의 코드를 보고 확인합니다.
 *   <li>안전장치: 주소는 서비스 형식만 받고 서버가 아이디로 공식 주소를 만들어 읽습니다. 한 코드로 확인 10번, 코드 발급은 하루 5번까지입니다.
 *   <li>같은 계정을 다른 회원이 이미 등록했거나, 이미 로그인으로 연결된 서비스면 409로 막습니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ChannelBioCodeService {
  /** 코드를 새로 받을 수 있는 하루 횟수입니다. */
  static final int DAILY_ISSUE_LIMIT = 5;

  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final ChannelBioCodeRepository codeRepository;
  private final MemberChannelConnectionRepository connectionRepository;
  private final ChannelBioReader reader;
  private final ChannelConnectionService connectionService;
  private final ChannelEvidenceStorage evidenceStorage;
  private final MemberService memberService;
  private final AuditLogService auditLogService;

  /** 지금 쓸 수 있는 코드입니다. 없으면 null입니다. */
  @Transactional(readOnly = true)
  public ChannelConnectDtos.BioCode current(Long memberId, String slug) {
    ChannelProvider provider = provider(slug);
    return codeRepository
        .findFirstByMemberIdAndProviderOrderByCreatedAtDescIdDesc(memberId, provider)
        .filter(code -> code.isUsable(LocalDateTime.now()))
        .map(code -> view(code, provider))
        .orElse(null);
  }

  /**
   * 코드를 받습니다. 쓸 수 있는 코드가 있으면 같은 코드를 돌려줘 소개글을 다시 고치지 않아도 됩니다.
   *
   * @throws ResponseStatusException 유튜브 등 지원하지 않는 서비스면 400, 하루 발급 횟수를 넘으면 429
   */
  @Transactional
  public ChannelConnectDtos.BioCode issue(Long memberId, String slug) {
    ChannelProvider provider = provider(slug);
    LocalDateTime now = LocalDateTime.now();
    ChannelBioCode latest =
        codeRepository
            .findFirstByMemberIdAndProviderOrderByCreatedAtDescIdDesc(memberId, provider)
            .orElse(null);
    if (latest != null && latest.isUsable(now)) return view(latest, provider);
    if (codeRepository.countByMemberIdAndProviderAndCreatedAtAfter(
            memberId, provider, now.minusDays(1))
        >= DAILY_ISSUE_LIMIT) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "인증 코드는 하루 " + DAILY_ISSUE_LIMIT + "번까지 새로 받을 수 있습니다.");
    }
    ChannelBioCode code = codeRepository.save(new ChannelBioCode(memberId, provider, newCode()));
    return view(code, provider);
  }

  /**
   * 소개글에서 코드를 확인합니다. 코드를 찾지 못한 경우에도 확인 횟수는 남깁니다(그래서 그 오류에는 되돌리지 않습니다).
   *
   * @throws ResponseStatusException 코드가 없거나 다 썼으면 409, 주소 오류·없는 블로그·팔로워 수 누락은 400, 소개글에 코드가 없으면
   *     422, 네이버를 지금 읽을 수 없으면 502
   */
  @Transactional(noRollbackFor = ResponseStatusException.class)
  public ChannelConnectDtos.BioVerifyResult verify(
      Long memberId, String slug, ChannelConnectDtos.BioVerifyRequest request) {
    ChannelProvider provider = provider(slug);
    ChannelBioCode code =
        codeRepository
            .findFirstByMemberIdAndProviderOrderByCreatedAtDescIdDesc(memberId, provider)
            .filter(value -> value.isUsable(LocalDateTime.now()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "쓸 수 있는 인증 코드가 없습니다. 코드를 새로 받아 주세요."));
    ManualChannelAddress.Parsed address;
    try {
      address = ManualChannelAddress.parse(provider, request.profileUrl());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
    Long followerInput = request.followerCount();
    if (followerInput != null && (followerInput < 0 || followerInput > 1_000_000_000L)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "팔로워 수를 다시 확인해 주세요.");
    }
    MemberChannelConnection existing =
        connectionRepository.findByMemberIdAndProvider(memberId, provider).orElse(null);
    if (existing != null && existing.getConnectionMethod() == ChannelConnectionMethod.OAUTH) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "이미 로그인으로 연결된 " + provider.label() + " 계정이 있습니다. 해제한 뒤 등록해 주세요.");
    }
    connectionRepository
        .findByProviderAndExternalId(provider, address.externalId())
        .filter(other -> !other.getMember().getId().equals(memberId))
        .ifPresent(
            other -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "이미 다른 MOVRIT 회원이 등록한 " + provider.label() + " 계정입니다.");
            });

    code.recordCheck();
    codeRepository.save(code);
    BioReadResult bio = reader.read(provider, accountId(address));
    switch (bio.status()) {
      case NOT_FOUND ->
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, provider.label() + " 계정을 찾을 수 없습니다. 주소를 확인해 주세요.");
      case UNAVAILABLE -> {
        if (provider != ChannelProvider.INSTAGRAM) {
          throw new ResponseStatusException(
              HttpStatus.BAD_GATEWAY, provider.label() + " 소개글을 지금 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        }
        return requestReview(memberId, provider, code, address, existing, followerInput);
      }
      case FOUND -> {
        if (!containsCode(bio.bio(), code.getCode())) {
          throw new ResponseStatusException(
              HttpStatus.UNPROCESSABLE_ENTITY,
              "소개글에서 인증 코드를 찾지 못했습니다. 소개글을 저장했는지 확인하고 다시 시도해 주세요(남은 확인 "
                  + code.checksLeft()
                  + "회).");
        }
        return verified(memberId, provider, code, address, existing, bio.followerCount());
      }
      default -> throw new IllegalStateException("알 수 없는 확인 결과입니다.");
    }
  }

  /** 소개글에 코드가 있는지 봅니다. 띄어쓰기·대소문자 차이는 무시합니다. */
  static boolean containsCode(String bio, String code) {
    if (bio == null || code == null) return false;
    String normalized = bio.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    return normalized.contains(code.toUpperCase(Locale.ROOT));
  }

  /** 새 코드입니다. 헷갈리는 글자(0·O·1·I)를 빼고 6자리로 만듭니다. 예) MOVRIT-7K3QX9 */
  String newCode() {
    for (int attempt = 0; attempt < 10; attempt++) {
      StringBuilder value = new StringBuilder("MOVRIT-");
      for (int i = 0; i < 6; i++) value.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
      if (!codeRepository.existsByCode(value.toString())) return value.toString();
    }
    throw new IllegalStateException("인증 코드를 만들지 못했습니다.");
  }

  private ChannelConnectDtos.BioVerifyResult verified(
      Long memberId,
      ChannelProvider provider,
      ChannelBioCode code,
      ManualChannelAddress.Parsed address,
      MemberChannelConnection existing,
      Long followerCount) {
    MemberChannelConnection connection = orNew(existing, memberId, provider);
    String oldEvidence = connection.getEvidenceKey();
    connection.verifyByBioCode(
        address.externalId(),
        address.accountName(),
        address.profileUrl(),
        followerCount,
        code.getCode());
    connectionRepository.save(connection);
    code.markUsed();
    evidenceStorage.delete(oldEvidence);
    connectionService.recordVerifiedMetric(memberId, provider, followerCount);
    audit(memberId, connection, "VERIFIED");
    return new ChannelConnectDtos.BioVerifyResult(
        "VERIFIED",
        provider.label() + " 채널을 확인했습니다. 이제 소개글에서 코드를 지워도 됩니다.",
        status(provider, connection));
  }

  private ChannelConnectDtos.BioVerifyResult requestReview(
      Long memberId,
      ChannelProvider provider,
      ChannelBioCode code,
      ManualChannelAddress.Parsed address,
      MemberChannelConnection existing,
      Long followerCount) {
    if (followerCount == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "자동으로 확인할 수 없는 계정이라 운영자가 확인합니다. 팔로워 수를 입력해 주세요.");
    }
    MemberChannelConnection connection = orNew(existing, memberId, provider);
    String oldEvidence = connection.getEvidenceKey();
    connection.requestBioCodeReview(
        address.externalId(),
        address.accountName(),
        address.profileUrl(),
        followerCount,
        code.getCode());
    connectionRepository.save(connection);
    code.markUsed();
    evidenceStorage.delete(oldEvidence);
    connectionService.recordMemberMetric(memberId, provider, followerCount);
    audit(memberId, connection, "PENDING");
    return new ChannelConnectDtos.BioVerifyResult(
        "PENDING_REVIEW",
        "이 계정은 자동으로 확인할 수 없어 운영자가 소개글의 코드를 확인합니다. 확인이 끝날 때까지 소개글의 코드를 지우지 말아 주세요.",
        status(provider, connection));
  }

  private MemberChannelConnection orNew(
      MemberChannelConnection existing, Long memberId, ChannelProvider provider) {
    return existing != null
        ? existing
        : MemberChannelConnection.builder()
            .member(memberService.getMember(memberId))
            .provider(provider)
            .build();
  }

  private ChannelConnectDtos.ProviderStatus status(
      ChannelProvider provider, MemberChannelConnection connection) {
    return new ChannelConnectDtos.ProviderStatus(
        provider.slug(), provider.label(), false, ChannelConnectDtos.Connection.from(connection));
  }

  private void audit(Long memberId, MemberChannelConnection connection, String status) {
    auditLogService.record(
        memberId,
        "MEMBER_CHANNEL_BIO_CODE",
        "MEMBER_CHANNEL_CONNECTION",
        connection.getId(),
        null,
        "{\"provider\":\"" + connection.getProvider() + "\",\"status\":\"" + status + "\"}",
        null);
  }

  private ChannelConnectDtos.BioCode view(ChannelBioCode code, ChannelProvider provider) {
    return new ChannelConnectDtos.BioCode(
        code.getCode(), code.getExpiresAt(), code.checksLeft(), reader.autoCheck(provider));
  }

  /** 검사를 마친 주소에서 계정 아이디만 꺼냅니다(externalId의 'manual:' 접두사 제거). */
  private static String accountId(ManualChannelAddress.Parsed address) {
    String id = address.externalId();
    return id.startsWith("manual:") ? id.substring("manual:".length()) : id;
  }

  private static ChannelProvider provider(String slug) {
    ChannelProvider provider =
        ChannelProvider.fromSlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지원하지 않는 채널입니다."));
    if (provider != ChannelProvider.INSTAGRAM && provider != ChannelProvider.NAVER_BLOG) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "소개글 코드 인증은 인스타그램과 네이버 블로그만 쓸 수 있습니다.");
    }
    return provider;
  }
}
