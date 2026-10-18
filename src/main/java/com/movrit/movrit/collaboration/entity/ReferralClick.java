package com.movrit.movrit.collaboration.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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
