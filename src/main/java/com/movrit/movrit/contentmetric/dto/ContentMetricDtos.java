package com.movrit.movrit.contentmetric.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** 콘텐츠 성과 API의 요청·응답 형식입니다. */
public final class ContentMetricDtos {
  private ContentMetricDtos() {}

  /** 숫자는 한 게시물 기준 10억 이하로 받습니다(잘못 붙여 넣은 큰 숫자를 막기 위해). */
  public static final int MAX_COUNT = 1_000_000_000;

  /**
   * 성과 입력입니다.
   *
   * @param contentUrl 제출한 게시물 주소 중 하나
   * @param measuredOn 숫자를 확인한 날짜. 비우면 오늘입니다.
   */
  public record RecordRequest(
      @NotBlank @Size(max = 1000) String contentUrl,
      @NotNull @Min(0) @Max(MAX_COUNT) Integer viewCount,
      @NotNull @Min(0) @Max(MAX_COUNT) Integer likeCount,
      @NotNull @Min(0) @Max(MAX_COUNT) Integer commentCount,
      LocalDate measuredOn) {}

  /** 하루치 기록입니다. recordedByRole은 CREATOR(참여자)·SELLER(업체)입니다. */
  public record Point(
      LocalDate measuredOn,
      int viewCount,
      int likeCount,
      int commentCount,
      String recordedByRole) {}

  /** 게시물 하나와 그 기록입니다. latest는 가장 최근 기록이고, 없으면 null입니다. */
  public record LinkMetrics(
      String channelType, String contentUrl, Point latest, List<Point> history) {}

  /** 제출 한 건의 성과입니다. editable은 지금 사용자가 입력할 수 있는지입니다. */
  public record SubmissionMetrics(
      Long submissionId,
      Long applicationId,
      String campaignTitle,
      boolean editable,
      LocalDate earliestDate,
      List<LinkMetrics> links) {}

  /** 캠페인 성과 요약의 게시물 한 줄입니다. */
  public record CampaignRow(
      Long applicationId,
      Long submissionId,
      String creatorNickname,
      String submissionStatus,
      String channelType,
      String contentUrl,
      Point latest) {}

  /** 캠페인 성과 요약입니다. 합계는 게시물마다 가장 최근 기록을 더한 값입니다. */
  public record CampaignPerformance(
      Long campaignId,
      String campaignTitle,
      int linkCount,
      int recordedLinkCount,
      long totalViews,
      long totalLikes,
      long totalComments,
      List<CampaignRow> rows) {}
}
