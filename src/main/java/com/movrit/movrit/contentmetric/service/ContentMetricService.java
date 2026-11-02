package com.movrit.movrit.contentmetric.service;

import com.movrit.movrit.campaigncontent.entity.CampaignContentLink;
import com.movrit.movrit.campaigncontent.entity.CampaignContentSubmission;
import com.movrit.movrit.campaigncontent.repository.CampaignContentLinkRepository;
import com.movrit.movrit.campaigncontent.repository.CampaignContentSubmissionRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.contentmetric.dto.ContentMetricDtos;
import com.movrit.movrit.contentmetric.entity.CampaignContentMetric;
import com.movrit.movrit.contentmetric.repository.CampaignContentMetricRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 제출한 콘텐츠의 성과(조회수·좋아요·댓글 수)를 기록하고 보여 줍니다.
 *
 * <ul>
 *   <li>입력: 그 콘텐츠를 낸 참여자와 캠페인 업체가 할 수 있습니다. 운영자는 보기만 합니다.
 *   <li>게시물: 지금 제출에 들어 있는 주소만 받습니다(재제출로 빠진 주소의 예전 기록은 그대로 둡니다).
 *   <li>날짜: 비우면 오늘이고, 미래 날짜나 콘텐츠를 내기 전 날짜는 받지 않습니다. 같은 게시물·같은 날짜를 다시 입력하면 고칩니다.
 *   <li>캠페인 요약: 게시물마다 가장 최근 기록을 모아 더합니다. 업체(캠페인 주인)와 운영자만 봅니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ContentMetricService {
  /** 게시물 하나에 보여 줄 기록 수입니다. */
  static final int HISTORY_LIMIT = 10;

  private final CampaignContentMetricRepository metricRepository;
  private final CampaignContentSubmissionRepository submissionRepository;
  private final CampaignContentLinkRepository linkRepository;
  private final CampaignRepository campaignRepository;
  private final MemberService memberService;

  /** 보는 사람과 콘텐츠의 관계입니다. */
  enum Viewer {
    CREATOR,
    SELLER,
    ADMIN
  }

  /** 제출 한 건의 게시물별 성과입니다. */
  @Transactional(readOnly = true)
  public ContentMetricDtos.SubmissionMetrics read(Long memberId, Long submissionId) {
    CampaignContentSubmission submission = getSubmission(submissionId);
    Viewer viewer = viewer(memberId, submission);
    return view(submission, viewer);
  }

  /**
   * 성과를 기록합니다.
   *
   * @throws ResponseStatusException 볼 수 없는 콘텐츠면 404, 운영자는 403, 제출에 없는 주소·잘못된 날짜는 400
   */
  @Transactional
  public ContentMetricDtos.SubmissionMetrics record(
      Long memberId, Long submissionId, ContentMetricDtos.RecordRequest request) {
    return record(memberId, submissionId, request, LocalDate.now());
  }

  ContentMetricDtos.SubmissionMetrics record(
      Long memberId, Long submissionId, ContentMetricDtos.RecordRequest request, LocalDate today) {
    CampaignContentSubmission submission = getSubmission(submissionId);
    Viewer viewer = viewer(memberId, submission);
    if (viewer == Viewer.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영자는 성과를 볼 수만 있습니다.");
    }
    String url = request.contentUrl().trim();
    Map<String, String> links = currentLinks(submission);
    String channel = links.get(url);
    if (channel == null) throw badRequest("제출한 게시물 주소 중에서 골라 주세요.");
    LocalDate measuredOn = request.measuredOn() == null ? today : request.measuredOn();
    if (measuredOn.isAfter(today)) throw badRequest("오늘 이후 날짜는 기록할 수 없습니다.");
    LocalDate earliest = earliestDate(submission);
    if (earliest != null && measuredOn.isBefore(earliest)) {
      throw badRequest("콘텐츠를 제출한 날(" + earliest + ") 이후 날짜로 기록해 주세요.");
    }
    CampaignContentMetric metric =
        metricRepository
            .findBySubmissionIdAndContentUrlAndMeasuredOn(submission.getId(), url, measuredOn)
            .orElseGet(
                () -> new CampaignContentMetric(submission.getId(), channel, url, measuredOn));
    metric.record(
        request.viewCount(),
        request.likeCount(),
        request.commentCount(),
        memberId,
        viewer == Viewer.CREATOR ? CampaignContentMetric.CREATOR : CampaignContentMetric.SELLER);
    metricRepository.save(metric);
    return view(submission, viewer);
  }

  /** 캠페인 한 개의 성과 요약입니다(업체·운영자). */
  @Transactional(readOnly = true)
  public ContentMetricDtos.CampaignPerformance campaign(Long memberId, Long campaignId) {
    Campaign campaign =
        campaignRepository
            .findById(campaignId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    Member member = memberService.getMember(memberId);
    if (!member.isAdmin() && !isCampaignOwner(memberId, campaign)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다.");
    }
    List<CampaignContentSubmission> submissions =
        submissionRepository.findAllByApplicationCampaignIdOrderByIdAsc(campaignId);
    List<Long> ids = submissions.stream().map(CampaignContentSubmission::getId).toList();
    Map<Long, List<CampaignContentLink>> linksBySubmission =
        ids.isEmpty()
            ? Map.of()
            : linkRepository.findAllBySubmissionIdInOrderBySubmissionIdAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(link -> link.getSubmission().getId()));
    Map<String, CampaignContentMetric> latest = new LinkedHashMap<>();
    if (!ids.isEmpty()) {
      // 최근 날짜순으로 읽으므로 게시물마다 처음 만난 기록이 가장 최근 기록입니다.
      for (CampaignContentMetric metric :
          metricRepository.findAllBySubmissionIdInOrderByMeasuredOnDescIdDesc(ids)) {
        latest.putIfAbsent(key(metric.getSubmissionId(), metric.getContentUrl()), metric);
      }
    }
    List<ContentMetricDtos.CampaignRow> rows = new ArrayList<>();
    for (CampaignContentSubmission submission : submissions) {
      for (Map.Entry<String, String> link :
          linkMap(submission, linksBySubmission.getOrDefault(submission.getId(), List.of()))
              .entrySet()) {
        CampaignContentMetric metric = latest.get(key(submission.getId(), link.getKey()));
        rows.add(
            new ContentMetricDtos.CampaignRow(
                submission.getApplication().getId(),
                submission.getId(),
                submission.getApplication().getCreator().getNickname(),
                submission.getStatus().name(),
                link.getValue(),
                link.getKey(),
                metric == null ? null : point(metric)));
      }
    }
    return summarize(campaign.getId(), campaign.getTitle(), rows);
  }

  // ───────── 규칙 ─────────

  /** 게시물마다 가장 최근 기록을 더합니다. 기록이 없는 게시물은 0으로 봅니다. */
  static ContentMetricDtos.CampaignPerformance summarize(
      Long campaignId, String title, List<ContentMetricDtos.CampaignRow> rows) {
    long views = 0;
    long likes = 0;
    long comments = 0;
    int recorded = 0;
    for (ContentMetricDtos.CampaignRow row : rows) {
      if (row.latest() == null) continue;
      recorded += 1;
      views += row.latest().viewCount();
      likes += row.latest().likeCount();
      comments += row.latest().commentCount();
    }
    return new ContentMetricDtos.CampaignPerformance(
        campaignId, title, rows.size(), recorded, views, likes, comments, rows);
  }

  private ContentMetricDtos.SubmissionMetrics view(
      CampaignContentSubmission submission, Viewer viewer) {
    Map<String, List<CampaignContentMetric>> byUrl =
        metricRepository
            .findAllBySubmissionIdOrderByMeasuredOnDescIdDesc(submission.getId())
            .stream()
            .collect(
                Collectors.groupingBy(
                    CampaignContentMetric::getContentUrl, LinkedHashMap::new, Collectors.toList()));
    List<ContentMetricDtos.LinkMetrics> links = new ArrayList<>();
    currentLinks(submission)
        .forEach(
            (url, channel) -> {
              List<ContentMetricDtos.Point> history =
                  byUrl.getOrDefault(url, List.of()).stream()
                      .limit(HISTORY_LIMIT)
                      .map(ContentMetricService::point)
                      .toList();
              links.add(
                  new ContentMetricDtos.LinkMetrics(
                      channel, url, history.isEmpty() ? null : history.get(0), history));
            });
    return new ContentMetricDtos.SubmissionMetrics(
        submission.getId(),
        submission.getApplication().getId(),
        submission.getApplication().getCampaign().getTitle(),
        viewer != Viewer.ADMIN,
        earliestDate(submission),
        links);
  }

  /** 지금 제출에 들어 있는 게시물 주소 → 채널입니다(입력 순서 유지). */
  private Map<String, String> currentLinks(CampaignContentSubmission submission) {
    return linkMap(
        submission, linkRepository.findAllBySubmissionIdOrderByIdAsc(submission.getId()));
  }

  /** 링크가 없는 예전 단일 주소 제출은 그 주소 하나를 '기타' 채널로 봅니다. */
  static Map<String, String> linkMap(
      CampaignContentSubmission submission, List<CampaignContentLink> links) {
    Map<String, String> result = new LinkedHashMap<>();
    for (CampaignContentLink link : links) {
      result.putIfAbsent(link.getContentUrl(), link.getChannelType().name());
    }
    if (result.isEmpty() && submission.getContentUrl() != null) {
      result.put(submission.getContentUrl(), "OTHER");
    }
    return result;
  }

  private Viewer viewer(Long memberId, CampaignContentSubmission submission) {
    Long creatorMemberId = submission.getApplication().getCreator().getMember().getId();
    if (Objects.equals(creatorMemberId, memberId)) return Viewer.CREATOR;
    if (isCampaignOwner(memberId, submission.getApplication().getCampaign())) return Viewer.SELLER;
    if (memberService.getMember(memberId).isAdmin()) return Viewer.ADMIN;
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다.");
  }

  private static boolean isCampaignOwner(Long memberId, Campaign campaign) {
    return Objects.equals(campaign.getBrand().getSeller().getMember().getId(), memberId);
  }

  private static LocalDate earliestDate(CampaignContentSubmission submission) {
    if (submission.getSubmittedAt() != null) return submission.getSubmittedAt().toLocalDate();
    return submission.getCreatedAt() == null ? null : submission.getCreatedAt().toLocalDate();
  }

  private CampaignContentSubmission getSubmission(Long submissionId) {
    return submissionRepository
        .findById(submissionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다."));
  }

  private static ContentMetricDtos.Point point(CampaignContentMetric metric) {
    return new ContentMetricDtos.Point(
        metric.getMeasuredOn(),
        metric.getViewCount(),
        metric.getLikeCount(),
        metric.getCommentCount(),
        metric.getRecordedByRole());
  }

  private static String key(Long submissionId, String url) {
    return submissionId + "|" + url;
  }

  private static ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
