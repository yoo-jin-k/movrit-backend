package com.movrit.movrit.collaboration.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "collaboration",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_collaboration_preorder_creator",
            columnNames = {"preorder_id", "creator_id"}))
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

  public void approve(String code) {
    if (status != CollaborationStatus.PENDING)
      throw new IllegalStateException("대기 중 협업만 승인할 수 있어.");
    status = CollaborationStatus.ACTIVE;
    referralCode = code;
    respondedAt = LocalDateTime.now();
  }

  public void reject() {
    if (status != CollaborationStatus.PENDING)
      throw new IllegalStateException("대기 중 협업만 거절할 수 있어.");
    status = CollaborationStatus.REJECTED;
    respondedAt = LocalDateTime.now();
  }

  public void acceptProposal(String code) {
    if (type != CollaborationType.PROPOSAL) throw new IllegalStateException("직접 제안만 수락할 수 있어.");
    approve(code);
  }

  public void end() {
    if (status != CollaborationStatus.ACTIVE) throw new IllegalStateException("진행 중 협업만 종료할 수 있어.");
    status = CollaborationStatus.ENDED;
  }

  public void cancel() {
    if (status == CollaborationStatus.ENDED || status == CollaborationStatus.REJECTED)
      throw new IllegalStateException("이미 종료된 협업입니다.");
    status = CollaborationStatus.CANCELLED;
  }
}
