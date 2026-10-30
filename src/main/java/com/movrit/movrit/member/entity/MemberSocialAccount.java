package com.movrit.movrit.member.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 회원과 소셜 로그인 계정(Google·카카오)의 연결 정보입니다. 같은 소셜 계정은 하나의 MOVRIT 회원에만 연결할 수 있습니다(제공자+제공자 ID 유일). */
@Entity
@Table(
    name = "member_social_account",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_social_provider_id",
          columnNames = {"provider", "provider_id"})
    })
/**
 * 회원 계정에 연결된 소셜 로그인 계정 한 건입니다.
 *
 * <p>제공자(GOOGLE·KAKAO)와 제공자 쪽 사용자 ID 쌍은 한 회원에게만 연결됩니다. 한 회원이 Google과 Kakao를 모두 연결해 같은 계정으로 로그인할 수
 * 있습니다.
 */
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
