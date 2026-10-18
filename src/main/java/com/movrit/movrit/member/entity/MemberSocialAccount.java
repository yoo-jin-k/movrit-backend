package com.movrit.movrit.member.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "member_social_account",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_social_provider_id",
          columnNames = {"provider", "provider_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberSocialAccount extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AuthProvider provider;

  @Column(name = "provider_id", nullable = false, length = 100)
  private String providerId;

  @Column(length = 100)
  private String email;
}
