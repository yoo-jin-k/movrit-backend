package com.movrit.movrit.participation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기간 연장 기록 한 건입니다. 신청 하나의 체험 기간·콘텐츠 마감을 늘릴 때마다 남깁니다.
 *
 * <ul>
 *   <li>PARTICIPANT_REQUEST: 참여자 요청을 업체(또는 운영자)가 승인한 연장입니다. 신청당 1회입니다.
 *   <li>SELLER / ADMIN: 업체나 운영자가 직접 늘린 연장입니다. 횟수 제한이 없습니다.
 * </ul>
 *
 * <p>운영자는 이 기록으로 참여자의 연장 횟수를 확인합니다(회원 상세).
 */
@Entity
@Table(name = "campaign_period_extension")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignPeriodExtension {
  public static final String PARTICIPANT_REQUEST = "PARTICIPANT_REQUEST";
  public static final String SELLER = "SELLER";
  public static final String ADMIN = "ADMIN";

  /** 직접 연장 한 번에 늘릴 수 있는 일수입니다. */
  public static final int MIN_DAYS = 1;

  public static final int MAX_DAYS = 30;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  /** 연장을 받은 참여자입니다. */
  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(nullable = false)
  private int days;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(nullable = false, length = 30)
  private String source;

  /** 승인하거나 직접 늘린 사람입니다. */
  @Column(name = "extended_by_member_id")
  private Long extendedByMemberId;

  @Column(name = "previous_experience_end")
  private LocalDateTime previousExperienceEnd;

  @Column(name = "new_experience_end")
  private LocalDateTime newExperienceEnd;

  @Column(name = "previous_deadline", nullable = false)
  private LocalDateTime previousDeadline;

  @Column(name = "new_deadline", nullable = false)
  private LocalDateTime newDeadline;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /** 이 연장이 포함되어 만들어진 연장 누적 패널티입니다. 비어 있으면 아직 패널티로 바뀌지 않았습니다. */
  @Column(name = "penalty_id")
  private Long penaltyId;

  public CampaignPeriodExtension(
      Long applicationId,
      Long memberId,
      int days,
      String reason,
      String source,
      Long extendedByMemberId,
      LocalDateTime previousExperienceEnd,
      LocalDateTime newExperienceEnd,
      LocalDateTime previousDeadline,
      LocalDateTime newDeadline) {
    this.applicationId = applicationId;
    this.memberId = memberId;
    this.days = days;
    this.reason = reason;
    this.source = source;
    this.extendedByMemberId = extendedByMemberId;
    this.previousExperienceEnd = previousExperienceEnd;
    this.newExperienceEnd = newExperienceEnd;
    this.previousDeadline = previousDeadline;
    this.newDeadline = newDeadline;
    this.createdAt = LocalDateTime.now();
  }

  /** 연장 횟수 기준으로 세는 기록인지 확인합니다. 1년 안이고 아직 패널티로 바뀌지 않은 것만 셉니다. */
  public boolean isCounting(LocalDateTime now, int validDays) {
    return penaltyId == null && createdAt.isAfter(now.minusDays(validDays));
  }

  /** 연장 누적 패널티로 바뀌었음을 기록합니다. */
  public void convertTo(Long penaltyId) {
    this.penaltyId = penaltyId;
  }

  /** 업체·운영자가 직접 늘린 연장인지 확인합니다. */
  public boolean isDirect() {
    return !PARTICIPANT_REQUEST.equals(source);
  }
}
