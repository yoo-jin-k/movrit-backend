package com.movrit.movrit.collaboration.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.preorder.entity.Preorder;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "collaboration",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_collaboration_preorder_creator",
            columnNames = {"preorder_id", "creator_id"}))
/**
 * 프리오더 상품과 크리에이터 사이의 협업 한 건입니다(기존 협업 기능). 크리에이터의 신청 또는 업체의 직접 제안으로 만들어지고, 승인되면 추천 코드가 발급됩니다. 한
 * 프리오더에 같은 크리에이터의 협업은 하나만 존재할 수 있습니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Collaboration extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "preorder_id", nullable = false)
  private Preorder preorder;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "creator_id", nullable = false)
  private Creator creator;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CollaborationType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CollaborationStatus status;

  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  @Column(name = "referral_code", unique = true, length = 40)
  private String referralCode;

  @Column(name = "responded_at")
  private LocalDateTime respondedAt;

  /** 대기 중인 협업을 승인해 진행 중(ACTIVE)으로 바꾸고 추천 코드를 저장합니다. 대기(PENDING) 상태가 아니면 승인할 수 없습니다. */
  public void approve(String code) {
    if (status != CollaborationStatus.PENDING)
      throw new IllegalStateException("대기 중 협업만 승인할 수 있습니다.");
    status = CollaborationStatus.ACTIVE;
    referralCode = code;
    respondedAt = LocalDateTime.now();
  }

  /** 대기 중인 협업을 거절(REJECTED)하고 응답 시각을 기록합니다. 대기 상태가 아니면 거절할 수 없습니다. */
  public void reject() {
    if (status != CollaborationStatus.PENDING)
      throw new IllegalStateException("대기 중 협업만 거절할 수 있습니다.");
    status = CollaborationStatus.REJECTED;
    respondedAt = LocalDateTime.now();
  }

  /** 업체가 보낸 직접 제안(PROPOSAL)을 크리에이터가 수락합니다. 신청으로 만든 협업에는 사용할 수 없습니다. */
  public void acceptProposal(String code) {
    if (type != CollaborationType.PROPOSAL) throw new IllegalStateException("직접 제안만 수락할 수 있습니다.");
    approve(code);
  }

  /** 진행 중인 협업을 종료(ENDED)합니다. 진행 중이 아니면 종료할 수 없습니다. */
  public void end() {
    if (status != CollaborationStatus.ACTIVE)
      throw new IllegalStateException("진행 중 협업만 종료할 수 있습니다.");
    status = CollaborationStatus.ENDED;
  }

  /** 협업을 취소(CANCELLED)합니다. 이미 종료되었거나 거절된 협업은 취소할 수 없습니다. */
  public void cancel() {
    if (status == CollaborationStatus.ENDED || status == CollaborationStatus.REJECTED)
      throw new IllegalStateException("이미 종료된 협업입니다.");
    status = CollaborationStatus.CANCELLED;
  }
}
