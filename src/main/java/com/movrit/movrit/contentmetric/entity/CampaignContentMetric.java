package com.movrit.movrit.contentmetric.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 콘텐츠 게시물 하나의 하루치 성과(조회수·좋아요·댓글 수)입니다.
 *
 * <p>외부 SNS 지표를 자동으로 가져오지 않으므로 참여자(CREATOR)나 캠페인 업체(SELLER)가 확인한 숫자를 입력합니다. 같은 게시물·같은 날짜는 한 줄이고, 다시
 * 입력하면 숫자와 입력한 사람만 바뀝니다.
 */
@Entity
@Table(name = "campaign_content_metric")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignContentMetric extends BaseEntity {
  public static final String CREATOR = "CREATOR";
  public static final String SELLER = "SELLER";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "submission_id", nullable = false)
  private Long submissionId;

  @Column(name = "channel_type", nullable = false, length = 30)
  private String channelType;

  @Column(name = "content_url", nullable = false, length = 1000)
  private String contentUrl;

  @Column(name = "view_count", nullable = false)
  private int viewCount;

  @Column(name = "like_count", nullable = false)
  private int likeCount;

  @Column(name = "comment_count", nullable = false)
  private int commentCount;

  @Column(name = "measured_on", nullable = false)
  private LocalDate measuredOn;

  @Column(name = "recorded_by_member_id")
  private Long recordedByMemberId;

  @Column(name = "recorded_by_role", nullable = false, length = 20)
  private String recordedByRole;

  public CampaignContentMetric(
      Long submissionId, String channelType, String contentUrl, LocalDate measuredOn) {
    this.submissionId = submissionId;
    this.channelType = channelType;
    this.contentUrl = contentUrl;
    this.measuredOn = measuredOn;
  }

  /** 숫자를 기록합니다. 처음 입력과 같은 날 다시 입력할 때 모두 씁니다. */
  public void record(int views, int likes, int comments, Long memberId, String role) {
    this.viewCount = views;
    this.likeCount = likes;
    this.commentCount = comments;
    this.recordedByMemberId = memberId;
    this.recordedByRole = role;
  }
}
