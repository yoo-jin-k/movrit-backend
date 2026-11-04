package com.movrit.movrit.channelconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.channelconnect.dto.ChannelConnectDtos;
import com.movrit.movrit.channelconnect.entity.ChannelBioCode;
import com.movrit.movrit.channelconnect.entity.ChannelConnectionMethod;
import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import com.movrit.movrit.channelconnect.entity.ChannelVerificationStatus;
import com.movrit.movrit.channelconnect.entity.MemberChannelConnection;
import com.movrit.movrit.channelconnect.repository.ChannelBioCodeRepository;
import com.movrit.movrit.channelconnect.repository.MemberChannelConnectionRepository;
import com.movrit.movrit.channelconnect.service.ChannelBioReader.BioReadResult;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** 소개글 인증 코드의 발급·확인·운영자 확인 넘김 규칙을 SNS 통신 없이 확인합니다. */
class ChannelBioCodeServiceTest {
  private final ChannelBioCodeRepository codes = mock(ChannelBioCodeRepository.class);
  private final MemberChannelConnectionRepository connections =
      mock(MemberChannelConnectionRepository.class);
  private final ChannelBioReader reader = mock(ChannelBioReader.class);
  private final ChannelConnectionService connectionService = mock(ChannelConnectionService.class);
  private final MemberService members = mock(MemberService.class);
  private final ChannelBioCodeService service =
      new ChannelBioCodeService(
          codes,
          connections,
          reader,
          connectionService,
          mock(ChannelEvidenceStorage.class),
          members,
          mock(AuditLogService.class));
  private final Member member = Member.builder().id(7L).name("참여자").build();

  private ChannelBioCode code(ChannelProvider provider) {
    ChannelBioCode value = new ChannelBioCode(7L, provider, "MOVRIT-7K3QX9");
    when(codes.findFirstByMemberIdAndProviderOrderByCreatedAtDescIdDesc(7L, provider))
        .thenReturn(Optional.of(value));
    when(connections.findByMemberIdAndProvider(7L, provider)).thenReturn(Optional.empty());
    when(connections.findByProviderAndExternalId(eq(provider), any())).thenReturn(Optional.empty());
    when(members.getMember(7L)).thenReturn(member);
    return value;
  }

  @Test
  void issueReturnsSameUsableCodeAndLimitsDailyIssues() {
    ChannelBioCode usable = code(ChannelProvider.NAVER_BLOG);
    assertThat(service.issue(7L, "naver-blog").code()).isEqualTo("MOVRIT-7K3QX9");
    verify(codes, never()).save(any());

    usable.markUsed();
    when(codes.countByMemberIdAndProviderAndCreatedAtAfter(
            eq(7L), eq(ChannelProvider.NAVER_BLOG), any()))
        .thenReturn(5L);
    assertThatThrownBy(() -> service.issue(7L, "naver-blog"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    // 유튜브는 소개글 코드를 쓰지 않습니다.
    assertThatThrownBy(() -> service.issue(7L, "youtube"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void newCodeIsReadable() {
    when(codes.existsByCode(any())).thenReturn(false);
    String value = service.newCode();
    assertThat(value).matches("MOVRIT-[A-HJ-NP-Z2-9]{6}");
  }

  @Test
  void naverBlogWithCodeInIntroIsVerifiedAtOnce() {
    ChannelBioCode issued = code(ChannelProvider.NAVER_BLOG);
    when(reader.read(ChannelProvider.NAVER_BLOG, "movrit_blog"))
        .thenReturn(BioReadResult.found("안녕하세요 movrit-7k3qx9 리뷰 블로그", null, "무브릿 블로그"));

    ChannelConnectDtos.BioVerifyResult result =
        service.verify(
            7L,
            "naver-blog",
            new ChannelConnectDtos.BioVerifyRequest("https://blog.naver.com/movrit_blog", null));

    assertThat(result.result()).isEqualTo("VERIFIED");
    ArgumentCaptor<MemberChannelConnection> saved =
        ArgumentCaptor.forClass(MemberChannelConnection.class);
    verify(connections).save(saved.capture());
    assertThat(saved.getValue().getConnectionMethod()).isEqualTo(ChannelConnectionMethod.BIO_CODE);
    assertThat(saved.getValue().getVerificationStatus())
        .isEqualTo(ChannelVerificationStatus.VERIFIED);
    assertThat(saved.getValue().getProfileUrl()).isEqualTo("https://blog.naver.com/movrit_blog");
    assertThat(issued.getUsedAt()).isNotNull();
  }

  @Test
  void missingCodeCountsTheCheckAndAnswers422() {
    ChannelBioCode issued = code(ChannelProvider.NAVER_BLOG);
    when(reader.read(ChannelProvider.NAVER_BLOG, "movrit_blog"))
        .thenReturn(BioReadResult.found("코드 없는 소개글", null, "무브릿 블로그"));

    assertThatThrownBy(
            () ->
                service.verify(
                    7L,
                    "naver-blog",
                    new ChannelConnectDtos.BioVerifyRequest("blog.naver.com/movrit_blog", null)))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    assertThat(issued.getCheckCount()).isEqualTo(1);
    assertThat(issued.getUsedAt()).isNull();
    verify(connections, never()).save(any());
    // 확인 10번을 다 쓰면 그 코드는 더 쓸 수 없습니다.
    ReflectionTestUtils.setField(issued, "checkCount", ChannelBioCode.MAX_CHECKS);
    assertThat(issued.isUsable(LocalDateTime.now())).isFalse();
  }

  @Test
  void instagramThatCannotBeCheckedGoesToAdminReviewWithCode() {
    code(ChannelProvider.INSTAGRAM);
    when(reader.read(ChannelProvider.INSTAGRAM, "movrit.daily"))
        .thenReturn(BioReadResult.unavailable());
    ChannelConnectDtos.BioVerifyRequest withoutFollowers =
        new ChannelConnectDtos.BioVerifyRequest("https://www.instagram.com/movrit.daily", null);

    // 운영자 확인으로 넘길 때는 팔로워 수가 필요합니다.
    assertThatThrownBy(() -> service.verify(7L, "instagram", withoutFollowers))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

    ChannelConnectDtos.BioVerifyResult result =
        service.verify(
            7L,
            "instagram",
            new ChannelConnectDtos.BioVerifyRequest(
                "https://www.instagram.com/movrit.daily", 1500L));
    assertThat(result.result()).isEqualTo("PENDING_REVIEW");
    ArgumentCaptor<MemberChannelConnection> saved =
        ArgumentCaptor.forClass(MemberChannelConnection.class);
    verify(connections).save(saved.capture());
    assertThat(saved.getValue().getVerificationStatus())
        .isEqualTo(ChannelVerificationStatus.PENDING);
    assertThat(saved.getValue().getVerificationCode()).isEqualTo("MOVRIT-7K3QX9");
    verify(connectionService).recordMemberMetric(7L, ChannelProvider.INSTAGRAM, 1500L);
    // 운영자는 이 등록을 직접 등록처럼 확인할 수 있습니다.
    saved.getValue().approve(1L);
    assertThat(saved.getValue().getVerificationStatus())
        .isEqualTo(ChannelVerificationStatus.VERIFIED);
  }

  @Test
  void instagramBusinessAccountRecordsVerifiedFollowers() {
    code(ChannelProvider.INSTAGRAM);
    when(reader.read(ChannelProvider.INSTAGRAM, "movrit.daily"))
        .thenReturn(BioReadResult.found("협업 문의 DM · MOVRIT-7K3QX9", 3200L, null));

    ChannelConnectDtos.BioVerifyResult result =
        service.verify(
            7L,
            "instagram",
            new ChannelConnectDtos.BioVerifyRequest("instagram.com/movrit.daily", null));

    assertThat(result.result()).isEqualTo("VERIFIED");
    assertThat(result.status().connection().followerCount()).isEqualTo(3200L);
    verify(connectionService).recordVerifiedMetric(7L, ChannelProvider.INSTAGRAM, 3200L);
  }

  @Test
  void accountOwnedByAnotherMemberIsRejected() {
    code(ChannelProvider.NAVER_BLOG);
    Member other = Member.builder().id(9L).name("다른 회원").build();
    when(connections.findByProviderAndExternalId(ChannelProvider.NAVER_BLOG, "manual:movrit_blog"))
        .thenReturn(
            Optional.of(
                MemberChannelConnection.builder()
                    .member(other)
                    .provider(ChannelProvider.NAVER_BLOG)
                    .build()));

    assertThatThrownBy(
            () ->
                service.verify(
                    7L,
                    "naver-blog",
                    new ChannelConnectDtos.BioVerifyRequest(
                        "https://blog.naver.com/movrit_blog", null)))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    verify(reader, never()).read(any(), any());
    verify(connectionService, never()).recordVerifiedMetric(anyLong(), any(), any());
  }

  @Test
  void naverRssIntroIsReadAndEmptyBlogIsNotFound() {
    String rss =
        "<?xml version=\"1.0\"?><rss version=\"2.0\"><channel>"
            + "<title><![CDATA[무브릿 블로그]]></title>"
            + "<description><![CDATA[체험 기록 &amp; 리뷰 · MOVRIT-7K3QX9]]></description>"
            + "<item><title>글</title><description><![CDATA[글 내용]]></description></item>"
            + "</channel></rss>";
    BioReadResult found = HttpChannelBioReader.parseNaverRss(rss);
    assertThat(found.status()).isEqualTo(ChannelBioReader.Status.FOUND);
    assertThat(found.bio()).isEqualTo("체험 기록 & 리뷰 · MOVRIT-7K3QX9");
    assertThat(found.accountName()).isEqualTo("무브릿 블로그");
    assertThat(ChannelBioCodeService.containsCode(found.bio(), "MOVRIT-7K3QX9")).isTrue();
    assertThat(ChannelBioCodeService.containsCode("MOVRIT - 7K3 QX9", "MOVRIT-7K3QX9")).isTrue();
    assertThat(ChannelBioCodeService.containsCode("MOVRIT-7K3QX8", "MOVRIT-7K3QX9")).isFalse();

    String empty = "<rss version=\"2.0\"><channel><title/><link/><description/></channel></rss>";
    assertThat(HttpChannelBioReader.parseNaverRss(empty).status())
        .isEqualTo(ChannelBioReader.Status.NOT_FOUND);
    assertThat(HttpChannelBioReader.parseNaverRss("<html>").status())
        .isEqualTo(ChannelBioReader.Status.UNAVAILABLE);
  }
}
