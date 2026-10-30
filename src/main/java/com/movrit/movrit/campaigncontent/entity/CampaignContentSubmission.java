package com.movrit.movrit.campaigncontent.entity;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신청 한 건에 대한 콘텐츠 제출 기록입니다.
 *
 * <p>대표 링크와 메모, 검토 상태, 수정 횟수, 검토 의견을 보관합니다. 채널별 링크 목록은 {@link CampaignContentLink}에 따로 저장합니다.
 * {@code version}은 브랜드와 운영자가 동시에 검토할 때 나중 요청이 앞 결과를 덮어쓰지 않도록 막는 낙관적 잠금 값입니다.
 */
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

  /**
   * 수정 요청을 받은 콘텐츠를 다시 제출합니다.
   *
   * <p>수정 요청(REVISION_REQUESTED) 상태에서만 가능하며, 링크·메모를 새 값으로 바꾸고 수정 횟수를 1 올린 뒤 다시 검토 대기로 돌립니다. 이전 검토
   * 의견과 검토 시각은 지웁니다.
   *
   * @throws IllegalStateException 수정 요청 상태가 아닐 때
   */
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

  /**
   * 브랜드가 수정을 요청합니다. 검토 대기(SUBMITTED) 콘텐츠에만 가능하며, 의견과 검토 시각을 기록합니다.
   *
   * @throws IllegalStateException 검토 대기 상태가 아닐 때
   */
  public void requestRevision(String comment) {
    if (status != ContentSubmissionStatus.SUBMITTED) {
      throw new IllegalStateException("검토 대기 중인 콘텐츠에만 수정을 요청할 수 있습니다.");
    }
    status = ContentSubmissionStatus.REVISION_REQUESTED;
    reviewComment = comment;
    reviewedAt = LocalDateTime.now();
  }

  /**
   * 브랜드가 콘텐츠 확인을 마칩니다(BRAND_APPROVED).
   *
   * <p>브랜드 확인만으로는 캠페인이 완료되지 않고, 운영자 최종 승인을 거쳐야 완료됩니다.
   *
   * @throws IllegalStateException 검토 대기 상태가 아닐 때
   */
  public void approveByBrand(String comment) {
    if (status != ContentSubmissionStatus.SUBMITTED) {
      throw new IllegalStateException("검토 대기 중인 콘텐츠만 승인할 수 있습니다.");
    }
    status = ContentSubmissionStatus.BRAND_APPROVED;
    reviewComment = comment;
    reviewedAt = LocalDateTime.now();
  }

  /**
   * 운영자가 수정을 요청합니다. 검토 대기 또는 브랜드 확인 완료 상태에서 가능합니다.
   *
   * @throws IllegalStateException 이미 최종 승인되었거나 수정 요청 중일 때
   */
  public void requestRevisionByOperator(String comment) {
    if (status != ContentSubmissionStatus.SUBMITTED
        && status != ContentSubmissionStatus.BRAND_APPROVED) {
      throw new IllegalStateException("검토 중인 콘텐츠에만 수정을 요청할 수 있습니다.");
    }
    status = ContentSubmissionStatus.REVISION_REQUESTED;
    reviewComment = comment;
    reviewedAt = LocalDateTime.now();
  }

  /**
   * 운영자가 최종 승인합니다(APPROVED). 검토 대기 또는 브랜드 확인 완료 상태에서 가능합니다.
   *
   * @throws IllegalStateException 이미 최종 승인되었거나 수정 요청 중일 때
   */
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
