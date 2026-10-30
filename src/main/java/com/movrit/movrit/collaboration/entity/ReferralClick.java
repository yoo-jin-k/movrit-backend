package com.movrit.movrit.collaboration.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 크리에이터 추천 링크가 눌린 기록 한 건입니다. 협업별 유입 수를 집계할 때 사용합니다. */
@Entity
@Table(name = "referral_click")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReferralClick extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "collaboration_id", nullable = false)
  private Collaboration collaboration;

  @Column(name = "visitor_key", length = 100)
  private String visitorKey;

  @Column(name = "user_agent", length = 500)
  private String userAgent;
}
