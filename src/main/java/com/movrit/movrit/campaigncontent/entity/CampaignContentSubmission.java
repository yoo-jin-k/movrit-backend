package com.movrit.movrit.campaigncontent.entity;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "campaign_content_submission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CampaignContentSubmission extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false, unique = true)
  private CampaignApplication application;

  @Column(name = "content_url", nullable = false, length = 1000)
  private String contentUrl;

  @Column(columnDefinition = "TEXT")
  private String note;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContentSubmissionStatus status;

  @Column(name = "revision_count", nullable = false)
  private Integer revisionCount;

  @Column(name = "review_comment", columnDefinition = "TEXT")
  private String reviewComment;

  @Column(name = "submitted_at", nullable = false)
  private LocalDateTime submittedAt;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  /** 수정 제출은 기존 이력을 덮어쓰되 횟수와 최신 제출 시각을 보존합니다. */
  public void resubmit(String contentUrl, String note) {
    if (status != ContentSubmissionStatus.REVISION_REQUESTED) {
      throw new IllegalStateException("수정 요청된 콘텐츠만 다시 제출할 수 있습니다.");
    }
    this.contentUrl = contentUrl;
    this.note = note;
    status = ContentSubmissionStatus.SUBMITTED;
    revisionCount += 1;
    reviewComment = null;
    reviewedAt = null;
    submittedAt = LocalDateTime.now();
  }

  public void requestRevision(String comment) {
    if (status != ContentSubmissionStatus.SUBMITTED) {
      throw new IllegalStateException("검토 대기 중인 콘텐츠에만 수정을 요청할 수 있습니다.");
    }
    status = ContentSubmissionStatus.REVISION_REQUESTED;
    reviewComment = comment;
    reviewedAt = LocalDateTime.now();
  }

  /** 브랜드 검토는 운영자 최종 승인과 분리해 캠페인을 바로 완료하지 않습니다. */
  public void approveByBrand(String comment) {
    if (status != ContentSubmissionStatus.SUBMITTED) {
      throw new IllegalStateException("검토 대기 중인 콘텐츠만 승인할 수 있습니다.");
    }
    status = ContentSubmissionStatus.BRAND_APPROVED;
    reviewComment = comment;
    reviewedAt = LocalDateTime.now();
  }

  public void requestRevisionByOperator(String comment) {
    if (status != ContentSubmissionStatus.SUBMITTED
        && status != ContentSubmissionStatus.BRAND_APPROVED) {
      throw new IllegalStateException("검토 중인 콘텐츠에만 수정을 요청할 수 있습니다.");
    }
    status = ContentSubmissionStatus.REVISION_REQUESTED;
    reviewComment = comment;
    reviewedAt = LocalDateTime.now();
  }

  public void approveByOperator(String comment) {
    if (status != ContentSubmissionStatus.SUBMITTED
        && status != ContentSubmissionStatus.BRAND_APPROVED) {
      throw new IllegalStateException("검토 중인 콘텐츠만 최종 승인할 수 있습니다.");
    }
    status = ContentSubmissionStatus.APPROVED;
    reviewComment = comment;
    reviewedAt = LocalDateTime.now();
  }
}
