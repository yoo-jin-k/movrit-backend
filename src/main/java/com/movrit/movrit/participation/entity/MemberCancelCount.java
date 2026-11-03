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
 * 참여자 취소 횟수 한 건입니다.
 *
 * <ul>
 *   <li>SELF_CANCEL: 선정된 뒤 참여자가 사정이 있어 바로 취소한 경우(단순 취소)
 *   <li>MISSED_DEADLINE: 콘텐츠 제출 마감까지 내지 않아 자동으로 취소된 경우
 * </ul>
 *
 * <p>1년이 지나면 세지 않고, 유효한 횟수가 5회가 되면 패널티 1회로 바뀝니다(바뀐 횟수는 penaltyId를 채워 다시 세지 않습니다). 협의 취소는 여기에 남기지
 * 않습니다.
 */
@Entity
@Table(name = "member_cancel_count")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCancelCount {
  public static final String SELF_CANCEL = "SELF_CANCEL";
  public static final String MISSED_DEADLINE = "MISSED_DEADLINE";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  @Column(name = "reason_type", nullable = false, length = 20)
  private String reasonType;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /** 이 횟수가 포함되어 만들어진 패널티입니다. 비어 있으면 아직 패널티로 바뀌지 않았습니다. */
  @Column(name = "penalty_id")
  private Long penaltyId;

  public MemberCancelCount(Long memberId, Long applicationId, String reasonType) {
    this.memberId = memberId;
    this.applicationId = applicationId;
    this.reasonType = reasonType;
    this.createdAt = LocalDateTime.now();
  }

  /** 지금 기준으로 세는 횟수인지 확인합니다. 1년 안이고 아직 패널티로 바뀌지 않은 것만 셉니다. */
  public boolean isCounting(LocalDateTime now, int validDays) {
    return penaltyId == null && createdAt.isAfter(now.minusDays(validDays));
  }

  /** 패널티로 바뀌었음을 기록합니다. */
  public void convertTo(Long penaltyId) {
    this.penaltyId = penaltyId;
  }
}
