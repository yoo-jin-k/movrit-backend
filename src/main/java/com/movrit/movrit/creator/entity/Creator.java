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

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "primary_category")
    private String primaryCategory;

    @Column(name = "status")
    private String status;
}
