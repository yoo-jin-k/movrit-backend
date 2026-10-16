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
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("모집 종료일은 시작일보다 뒤여야 합니다.");
        }
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
    }

    public void close() {
        open = false;
    }
}
