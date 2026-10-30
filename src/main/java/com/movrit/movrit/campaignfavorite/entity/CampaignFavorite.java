package com.movrit.movrit.campaignfavorite.entity;

import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 찜한 캠페인 한 건입니다.
 *
 * <p>같은 회원이 같은 캠페인을 두 번 찜할 수 없도록 DB에 유니크 제약이 있습니다. 찜한 시각은 BaseEntity의 생성 시각(createdAt)을 사용합니다.
 */
@Entity
@Table(
    name = "campaign_favorite",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_campaign_favorite_member_campaign",
            columnNames = {"member_id", "campaign_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CampaignFavorite extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 찜한 회원입니다. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  /** 찜한 캠페인입니다. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false)
  private Campaign campaign;
}
