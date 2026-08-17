package com.movrit.movrit.creator.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "influencer_campaign",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_influencer_campaign_creator_preorder",
                columnNames = {"creator_id", "preorder_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CreatorCampaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preorder_id", nullable = false)
    private Preorder preorder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreatorCampaignStatus status;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    public void changeStatus(CreatorCampaignStatus status) {
        this.status = status;
    }
}
