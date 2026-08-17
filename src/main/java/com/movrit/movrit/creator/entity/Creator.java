package com.movrit.movrit.creator.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "influencer_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Creator extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "primary_category")
    private String primaryCategory;

    @Column(name = "channel_platform", length = 30)
    private String channelPlatform;

    @Column(name = "channel_handle", length = 100)
    private String channelHandle;

    @Column(name = "channel_url", length = 500)
    private String channelUrl;

    @Column(name = "follower_count")
    private Long followerCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CreatorStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    public void updateProfile(String nickname, String bio, String primaryCategory,
                              String channelPlatform, String channelHandle, String channelUrl,
                              Long followerCount) {
        this.nickname = nickname;
        this.bio = bio;
        this.primaryCategory = primaryCategory;
        this.channelPlatform = channelPlatform;
        this.channelHandle = channelHandle;
        this.channelUrl = channelUrl;
        this.followerCount = followerCount;
    }

    public void approve() {
        this.status = CreatorStatus.APPROVED;
        this.rejectionReason = null;
    }

    public void reject(String rejectionReason) {
        this.status = CreatorStatus.REJECTED;
        this.rejectionReason = rejectionReason;
    }

    public void suspend() {
        this.status = CreatorStatus.SUSPENDED;
    }
}
