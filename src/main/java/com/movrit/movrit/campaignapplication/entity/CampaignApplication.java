package com.movrit.movrit.campaignapplication.entity;

import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.creator.entity.Creator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "campaign_application",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_campaign_application_campaign_creator",
            columnNames = {"campaign_id", "creator_id"}))
/**
 * 참여자가 캠페인에 낸 신청 한 건입니다.
 *
 * <p>체험 방식, 배송지, 질문 답변, 동의 내역을 신청 당시 그대로 복사해 보관합니다. 이후 회원 주소록이나 캠페인 조건이 바뀌어도 이 신청의 기록은 바뀌지 않습니다. 한
 * 참여자는 같은 캠페인에 한 번만 신청할 수 있습니다(campaign_id + creator_id 고유 제약). 상태 변경은 아래 메서드로만 하며, 허용되지 않은 단계에서
 * 호출하면 IllegalStateException을 던집니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CampaignApplication extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false)
  private Campaign campaign;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "creator_id", nullable = false)
  private Creator creator;

  @Enumerated(EnumType.STRING)
  @Column(name = "fulfillment_type", nullable = false)
  private FulfillmentType fulfillmentType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CampaignApplicationStatus status;

  @Column(columnDefinition = "TEXT")
  private String message;

  @Column(name = "recipient_name")
  private String recipientName;

  @Column(name = "recipient_phone")
  private String recipientPhone;

  @Column(name = "postal_code")
  private String postalCode;

  @Column(name = "address1", length = 300)
  private String address1;

  @Column(name = "address2", length = 300)
  private String address2;

  @Column(name = "answers_json", nullable = false, columnDefinition = "TEXT")
  private String answersJson;

  @Column(name = "agreement_acceptances_json", nullable = false, columnDefinition = "TEXT")
  private String agreementAcceptancesJson;

  /** 신청 당시 동의한 신청서 양식 버전입니다. */
  @Column(name = "form_version", nullable = false)
  private Long formVersion;

  /** 이후 조건이 바뀌어도 신청 당시 판정 근거를 보존합니다. */
  @Column(name = "eligibility_requirement_snapshot_json", columnDefinition = "TEXT")
  private String eligibilityRequirementSnapshotJson;

  @Column(name = "eligibility_requirement_version")
  private Long eligibilityRequirementVersion;

  @Column(name = "identity_verification_configured", nullable = false)
  private boolean identityVerificationConfigured;

  @Column(name = "identity_verified", nullable = false)
  private boolean identityVerified;

  /** 신청 시점에 서버가 확인한 이메일 소유 상태를 기록합니다. */
  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  /** 신청 당시 활동 프로필에 SNS 주소가 등록되어 있었는지 기록합니다. 참여 조건 충족 여부와는 다릅니다. */
  @Column(name = "channel_ready", nullable = false)
  private boolean channelReady;

  @Column(name = "submitted_at", nullable = false)
  private LocalDateTime submittedAt;

  @Column(name = "responded_at")
  private LocalDateTime respondedAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Column(name = "cancellation_reason", length = 500)
  private String cancellationReason;

  /** 참여자가 체험 완료(배송 수령 또는 방문)를 확인한 시각입니다. 이 값이 있어야 콘텐츠를 제출할 수 있습니다. */
  @Column(name = "experience_confirmed_at")
  private LocalDateTime experienceConfirmedAt;

  /** 신청을 선정합니다. 접수(SUBMITTED) 상태에서만 가능하며 선정(SELECTED)으로 바뀝니다. */
  public void select() {
    requireStatus(CampaignApplicationStatus.SUBMITTED, "접수된 신청만 선정할 수 있습니다.");
    status = CampaignApplicationStatus.SELECTED;
    respondedAt = LocalDateTime.now();
  }

  /** 신청을 미선정 처리합니다. 접수(SUBMITTED) 상태에서만 가능합니다. */
  public void reject() {
    requireStatus(CampaignApplicationStatus.SUBMITTED, "접수된 신청만 거절할 수 있습니다.");
    status = CampaignApplicationStatus.REJECTED;
    respondedAt = LocalDateTime.now();
  }

  /** 선정된 참여자가 참여를 수락합니다. 선정(SELECTED)에서 진행 중(ACTIVE)으로 바뀝니다. */
  public void activate() {
    requireStatus(CampaignApplicationStatus.SELECTED, "선정된 신청만 참여를 확정할 수 있습니다.");
    status = CampaignApplicationStatus.ACTIVE;
  }

  /**
   * 참여자가 체험 완료를 확인합니다. 배송형은 상품 수령, 방문형은 방문 완료를 뜻합니다.
   *
   * <p>진행 중(ACTIVE)에서만 가능하며 상태는 바뀌지 않고 확인 시각만 처음 한 번 기록합니다.
   */
  public void confirmExperience() {
    requireStatus(CampaignApplicationStatus.ACTIVE, "진행 중인 캠페인만 체험 완료를 확인할 수 있습니다.");
    if (experienceConfirmedAt == null) {
      experienceConfirmedAt = LocalDateTime.now();
    }
  }

  /**
   * 콘텐츠가 제출되었음을 신청에 반영합니다.
   *
   * <p>체험 완료 확인이 먼저 있어야 하며, 진행 중 또는 수정 요청 상태에서 '콘텐츠 제출'로 바뀝니다.
   */
  public void markContentSubmitted() {
    if (experienceConfirmedAt == null) {
      throw new IllegalStateException("체험 완료 확인 후 콘텐츠를 제출해 주세요.");
    }
    if (status != CampaignApplicationStatus.ACTIVE
        && status != CampaignApplicationStatus.REVISION_REQUESTED) {
      throw new IllegalStateException("진행 중이거나 수정 요청된 캠페인만 콘텐츠를 제출할 수 있습니다.");
    }
    status = CampaignApplicationStatus.CONTENT_SUBMITTED;
  }

  /** 제출된 콘텐츠에 수정 요청이 왔음을 반영합니다. 콘텐츠 제출 상태에서 '수정 요청'으로 바뀝니다. */
  public void requestContentRevision() {
    requireStatus(CampaignApplicationStatus.CONTENT_SUBMITTED, "제출된 콘텐츠에만 수정을 요청할 수 있습니다.");
    status = CampaignApplicationStatus.REVISION_REQUESTED;
  }

  /** 운영자 최종 승인으로 캠페인 참여를 완료합니다. 콘텐츠 제출 상태에서 '완료'로 바뀝니다. */
  public void completeContentReview() {
    requireStatus(CampaignApplicationStatus.CONTENT_SUBMITTED, "제출된 콘텐츠만 승인할 수 있습니다.");
    status = CampaignApplicationStatus.COMPLETED;
  }

  /**
   * 캠페인 참여를 취소합니다.
   *
   * <p>접수·선정·진행 중 상태에서만 가능하며, 콘텐츠를 제출한 뒤에는 취소할 수 없습니다. 취소 사유와 시각을 기록합니다. 캠페인 참여만 취소하고 주문·결제 데이터는
   * 건드리지 않습니다.
   */
  public void cancel(String reason) {
    if (status != CampaignApplicationStatus.SUBMITTED
        && status != CampaignApplicationStatus.SELECTED
        && status != CampaignApplicationStatus.ACTIVE) {
      throw new IllegalStateException("접수·선정·진행 상태에서만 참여를 취소할 수 있습니다.");
    }
    status = CampaignApplicationStatus.CANCELLED;
    cancellationReason = reason;
    cancelledAt = LocalDateTime.now();
  }

  private void requireStatus(CampaignApplicationStatus expected, String message) {
    if (status != expected) throw new IllegalStateException(message);
  }
}
