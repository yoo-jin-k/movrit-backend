package com.movrit.movrit.contentmetric.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaigncontent.entity.CampaignContentLink;
import com.movrit.movrit.campaigncontent.entity.CampaignContentSubmission;
import com.movrit.movrit.campaigncontent.entity.ContentChannel;
import com.movrit.movrit.campaigncontent.entity.ContentSubmissionStatus;
import com.movrit.movrit.campaigncontent.repository.CampaignContentLinkRepository;
import com.movrit.movrit.campaigncontent.repository.CampaignContentSubmissionRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.contentmetric.dto.ContentMetricDtos;
import com.movrit.movrit.contentmetric.entity.CampaignContentMetric;
import com.movrit.movrit.contentmetric.repository.CampaignContentMetricRepository;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.seller.entity.SellerProfile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 콘텐츠 성과 기록의 권한·게시물·날짜 규칙과 캠페인 합계를 확인합니다. */
class ContentMetricServiceTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);
  private static final String URL = "https://www.instagram.com/p/abc/";

  private final CampaignContentMetricRepository metrics =
      mock(CampaignContentMetricRepository.class);
  private final CampaignContentSubmissionRepository submissions =
      mock(CampaignContentSubmissionRepository.class);
  private final CampaignContentLinkRepository links = mock(CampaignContentLinkRepository.class);
  private final MemberService members = mock(MemberService.class);
  private final ContentMetricService service =
      new ContentMetricService(
          metrics, submissions, links, mock(CampaignRepository.class), members);

  private final Member creatorMember = Member.builder().id(1L).role(MemberRole.USER).build();
  private final Member sellerMember = Member.builder().id(2L).role(MemberRole.USER).build();
  private final Member admin = Member.builder().id(9L).role(MemberRole.ADMIN).build();
  private final Member stranger = Member.builder().id(5L).role(MemberRole.USER).build();
  private CampaignContentSubmission submission;
  private final List<CampaignContentMetric> saved = new ArrayList<>();

  @BeforeEach
  void setUp() {
    Campaign campaign =
        Campaign.builder()
            .id(10L)
            .title("앰플 체험단")
            .brand(
                Brand.builder()
                    .id(3L)
                    .seller(SellerProfile.builder().id(4L).member(sellerMember).build())
                    .build())
            .build();
    CampaignApplication application =
        CampaignApplication.builder()
            .id(20L)
            .campaign(campaign)
            .creator(Creator.builder().id(6L).member(creatorMember).nickname("하늘기록").build())
            .build();
    submission =
        CampaignContentSubmission.builder()
            .id(30L)
            .application(application)
            .contentUrl(URL)
            .status(ContentSubmissionStatus.APPROVED)
            .revisionCount(0)
            .submittedAt(LocalDateTime.of(2026, 10, 30, 10, 0))
            .build();
    when(submissions.findById(30L)).thenReturn(Optional.of(submission));
    when(links.findAllBySubmissionIdOrderByIdAsc(30L))
        .thenReturn(
            List.of(
                CampaignContentLink.builder()
                    .id(40L)
                    .submission(submission)
                    .channelType(ContentChannel.INSTAGRAM)
                    .contentUrl(URL)
                    .build()));
    when(members.getMember(1L)).thenReturn(creatorMember);
    when(members.getMember(2L)).thenReturn(sellerMember);
    when(members.getMember(9L)).thenReturn(admin);
    when(members.getMember(5L)).thenReturn(stranger);
    when(metrics.findBySubmissionIdAndContentUrlAndMeasuredOn(any(), any(), any()))
        .thenAnswer(
            invocation ->
                saved.stream()
                    .filter(m -> m.getMeasuredOn().equals(invocation.getArgument(2)))
                    .findFirst());
    when(metrics.save(any()))
        .thenAnswer(
            invocation -> {
              CampaignContentMetric metric = invocation.getArgument(0);
              if (!saved.contains(metric)) saved.add(0, metric);
              return metric;
            });
    when(metrics.findAllBySubmissionIdOrderByMeasuredOnDescIdDesc(30L)).thenReturn(saved);
  }

  private ContentMetricDtos.RecordRequest request(int views, LocalDate day) {
    return new ContentMetricDtos.RecordRequest(URL, views, 12, 3, day);
  }

  @Test
  void creatorRecordsAndSameDayUpdatesOneRow() {
    var first = service.record(1L, 30L, request(100, null), TODAY);
    assertThat(first.links())
        .singleElement()
        .satisfies(
            link -> {
              assertThat(link.channelType()).isEqualTo("INSTAGRAM");
              assertThat(link.latest().viewCount()).isEqualTo(100);
              assertThat(link.latest().measuredOn()).isEqualTo(TODAY);
              assertThat(link.latest().recordedByRole()).isEqualTo("CREATOR");
            });
    var second = service.record(2L, 30L, request(150, TODAY), TODAY);
    assertThat(saved).hasSize(1);
    assertThat(second.links().get(0).latest().viewCount()).isEqualTo(150);
    assertThat(second.links().get(0).latest().recordedByRole()).isEqualTo("SELLER");
  }

  @Test
  void rejectsUnknownUrlFutureDateAndDateBeforeSubmission() {
    assertThatThrownBy(
            () ->
                service.record(
                    1L,
                    30L,
                    new ContentMetricDtos.RecordRequest("https://other.example/", 1, 1, 1, null),
                    TODAY))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    assertThatThrownBy(() -> service.record(1L, 30L, request(1, TODAY.plusDays(1)), TODAY))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.record(1L, 30L, request(1, LocalDate.of(2026, 10, 29)), TODAY))
        .hasMessageContaining("2026-10-30");
  }

  @Test
  void adminReadsOnlyAndStrangerCannotSee() {
    assertThat(service.read(9L, 30L).editable()).isFalse();
    assertThatThrownBy(() -> service.record(9L, 30L, request(1, null), TODAY))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    assertThatThrownBy(() -> service.read(5L, 30L))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void campaignTotalsUseLatestRecordPerLink() {
    var recorded = new ContentMetricDtos.Point(TODAY, 300, 20, 4, "CREATOR");
    var rows =
        List.of(
            new ContentMetricDtos.CampaignRow(
                20L, 30L, "하늘기록", "APPROVED", "INSTAGRAM", URL, recorded),
            new ContentMetricDtos.CampaignRow(
                21L, 31L, "민지데일리", "SUBMITTED", "BLOG", "https://b/", null));
    var summary = ContentMetricService.summarize(10L, "앰플 체험단", rows);
    assertThat(summary.linkCount()).isEqualTo(2);
    assertThat(summary.recordedLinkCount()).isEqualTo(1);
    assertThat(summary.totalViews()).isEqualTo(300);
    assertThat(summary.totalLikes()).isEqualTo(20);
  }

  @Test
  void oldSingleUrlSubmissionIsTreatedAsOtherChannel() {
    var map = ContentMetricService.linkMap(submission, List.of());
    assertThat(map).containsEntry(URL, "OTHER");
  }
}
