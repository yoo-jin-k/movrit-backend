package com.movrit.movrit.campaignapplication.entity;

import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.creator.entity.Creator;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "campaign_application",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_campaign_application_campaign_creator",
                columnNames = {"campaign_id", "creator_id"}
        )
)
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

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "answers_json", nullable = false, columnDefinition = "TEXT")
    private String answersJson;

    @Column(name = "agreement_acceptances_json", nullable = false, columnDefinition = "TEXT")
    private String agreementAcceptancesJson;

    @Column(name = "form_version", nullable = false)
    private Long formVersion;

    @Column(name = "identity_verification_configured", nullable = false)
    private boolean identityVerificationConfigured;

    @Column(name = "identity_verified", nullable = false)
    private boolean identityVerified;

    @Column(name = "channel_ready", nullable = false)
    private boolean channelReady;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "experience_confirmed_at")
    private LocalDateTime experienceConfirmedAt;

    public void select() {
        requireStatus(CampaignApplicationStatus.SUBMITTED, "접수된 신청만 선정할 수 있습니다.");
        status = CampaignApplicationStatus.SELECTED;
        respondedAt = LocalDateTime.now();
    }

    public void reject() {
        requireStatus(CampaignApplicationStatus.SUBMITTED, "접수된 신청만 거절할 수 있습니다.");
        status = CampaignApplicationStatus.REJECTED;
        respondedAt = LocalDateTime.now();
    }

    public void activate() {
        requireStatus(CampaignApplicationStatus.SELECTED, "선정된 신청만 참여를 확정할 수 있습니다.");
        status = CampaignApplicationStatus.ACTIVE;
    }

    /** 배송형은 샘플 수령, 방문형은 방문 완료를 참여자가 직접 확인합니다. */
    public void confirmExperience() {
        requireStatus(CampaignApplicationStatus.ACTIVE, "진행 중인 캠페인만 체험 완료를 확인할 수 있습니다.");
        if (experienceConfirmedAt == null) {
            experienceConfirmedAt = LocalDateTime.now();
        }
    }

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

    public void requestContentRevision() {
        requireStatus(CampaignApplicationStatus.CONTENT_SUBMITTED, "제출된 콘텐츠에만 수정을 요청할 수 있습니다.");
        status = CampaignApplicationStatus.REVISION_REQUESTED;
    }

    public void completeContentReview() {
        requireStatus(CampaignApplicationStatus.CONTENT_SUBMITTED, "제출된 콘텐츠만 승인할 수 있습니다.");
        status = CampaignApplicationStatus.COMPLETED;
    }

    /** 금전 상태와 무관한 캠페인 참여만 취소하며 주문·결제 데이터는 변경하지 않습니다. */
    public void cancel(String reason) {
        if (status != CampaignApplicationStatus.SUBMITTED && status != CampaignApplicationStatus.SELECTED && status != CampaignApplicationStatus.ACTIVE) {
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
