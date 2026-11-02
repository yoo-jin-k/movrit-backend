package com.movrit.movrit.participation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업체가 완료한 참여자에게 남기는 평가(1~5점)와 메모입니다(신청당 1건, 고칠 수 있음).
 *
 * <p>점수 평균은 다른 브랜드가 참여자를 고를 때 참고하도록 보여주고, 메모는 남긴 브랜드만 봅니다. 참여자 본인에게는 보이지 않습니다.
 */
@Entity
@Table(name = "campaign_participant_rating")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantRating {
  @Id
  @Column(name = "application_id")
  private Long applicationId;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(nullable = false)
  private Integer rating;

  @Column(length = 1000)
  private String memo;

  @Column(name = "rated_by_member_id", nullable = false)
  private Long ratedByMemberId;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public ParticipantRating(Long applicationId, Long memberId) {
    this.applicationId = applicationId;
    this.memberId = memberId;
  }

  public void update(int rating, String memo, Long ratedBy) {
    this.rating = rating;
    this.memo = memo;
    this.ratedByMemberId = ratedBy;
    this.updatedAt = LocalDateTime.now();
  }
}
