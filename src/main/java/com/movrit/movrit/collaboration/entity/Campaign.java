package com.movrit.movrit.collaboration.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "collaboration_campaign")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Campaign extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preorder_id", nullable = false, unique = true)
    private Preorder preorder;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "min_follower_count", nullable = false)
    private Long minFollowerCount;

    @Column(name = "recruitment_count", nullable = false)
    private Integer recruitmentCount;

    @Column(name = "reward_description", columnDefinition = "TEXT")
    private String rewardDescription;

    @Column(columnDefinition = "TEXT")
    private String mission;

    @Column(name = "channel_guide", columnDefinition = "TEXT")
    private String channelGuide;

    @Column(name = "schedule_guide", columnDefinition = "TEXT")
    private String scheduleGuide;

    @Column(name = "delivery_guide", columnDefinition = "TEXT")
    private String deliveryGuide;

    @Column(name = "visit_guide", columnDefinition = "TEXT")
    private String visitGuide;

    @Column(columnDefinition = "TEXT")
    private String caution;

    @Column(name = "submission_requirement", columnDefinition = "TEXT")
    private String submissionRequirement;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "selection_announced_at")
    private LocalDateTime selectionAnnouncedAt;

    @Column(name = "experience_start_at")
    private LocalDateTime experienceStartAt;

    @Column(name = "experience_end_at")
    private LocalDateTime experienceEndAt;

    @Column(name = "content_submission_start_at")
    private LocalDateTime contentSubmissionStartAt;

    @Column(name = "content_submission_end_at")
    private LocalDateTime contentSubmissionEndAt;

    @Column(name = "results_announced_at")
    private LocalDateTime resultsAnnouncedAt;

    @Builder.Default
    @Column(name = "schedule_version", nullable = false)
    private Long scheduleVersion = 1L;

    @Column(nullable = false)
    private boolean open;

    /** 판매자가 모집 내용과 참여 안내를 한 번에 수정할 때 사용합니다. */
    public void update(
            String title,
            String description,
            String category,
            Long minFollowerCount,
            Integer recruitmentCount,
            String rewardDescription,
            String mission,
            String channelGuide,
            String scheduleGuide,
            String deliveryGuide,
            String visitGuide,
            String caution,
            String submissionRequirement,
            Long expectedScheduleVersion,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime selectionAnnouncedAt,
            LocalDateTime experienceStartAt,
            LocalDateTime experienceEndAt,
            LocalDateTime contentSubmissionStartAt,
            LocalDateTime contentSubmissionEndAt,
            LocalDateTime resultsAnnouncedAt
    ) {
        if (scheduleVersion == null || !scheduleVersion.equals(expectedScheduleVersion)) {
            throw new IllegalArgumentException("캠페인 일정이 변경되었습니다. 최신 일정을 다시 확인해 주세요.");
        }
        validateSchedule(startAt, endAt, selectionAnnouncedAt, experienceStartAt, experienceEndAt,
                contentSubmissionStartAt, contentSubmissionEndAt, resultsAnnouncedAt);
        this.title = title;
        this.description = description;
        this.category = category;
        this.minFollowerCount = minFollowerCount;
        this.recruitmentCount = recruitmentCount;
        this.rewardDescription = rewardDescription;
        this.mission = mission;
        this.channelGuide = channelGuide;
        this.scheduleGuide = scheduleGuide;
        this.deliveryGuide = deliveryGuide;
        this.visitGuide = visitGuide;
        this.caution = caution;
        this.submissionRequirement = submissionRequirement;
        this.startAt = startAt;
        this.endAt = endAt;
        this.selectionAnnouncedAt = selectionAnnouncedAt;
        this.experienceStartAt = experienceStartAt;
        this.experienceEndAt = experienceEndAt;
        this.contentSubmissionStartAt = contentSubmissionStartAt;
        this.contentSubmissionEndAt = contentSubmissionEndAt;
        this.resultsAnnouncedAt = resultsAnnouncedAt;
        this.scheduleVersion += 1L;
    }

    public void close() {
        open = false;
    }

    private void validateSchedule(
            LocalDateTime recruitmentStartAt,
            LocalDateTime recruitmentEndAt,
            LocalDateTime selectionAt,
            LocalDateTime experienceStart,
            LocalDateTime experienceEnd,
            LocalDateTime submissionStart,
            LocalDateTime submissionEnd,
            LocalDateTime resultsAt
    ) {
        if (!recruitmentStartAt.isBefore(recruitmentEndAt)) {
            throw new IllegalArgumentException("모집 종료일은 시작일보다 뒤여야 합니다.");
        }
        if (selectionAt != null && selectionAt.isBefore(recruitmentEndAt)) {
            throw new IllegalArgumentException("선정 발표일은 모집 종료일보다 빠를 수 없습니다.");
        }
        if (experienceStart != null && selectionAt != null && experienceStart.isBefore(selectionAt)) {
            throw new IllegalArgumentException("체험 시작일은 선정 발표일보다 빠를 수 없습니다.");
        }
        if (experienceStart != null && experienceEnd != null && !experienceStart.isBefore(experienceEnd)) {
            throw new IllegalArgumentException("체험 종료일은 체험 시작일보다 뒤여야 합니다.");
        }
        if (submissionStart != null && submissionEnd != null && !submissionStart.isBefore(submissionEnd)) {
            throw new IllegalArgumentException("콘텐츠 등록 종료일은 시작일보다 뒤여야 합니다.");
        }
        if (resultsAt != null && submissionEnd != null && resultsAt.isBefore(submissionEnd)) {
            throw new IllegalArgumentException("결과 발표일은 콘텐츠 등록 종료일보다 빠를 수 없습니다.");
        }
    }
}
