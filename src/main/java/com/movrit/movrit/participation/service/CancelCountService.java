package com.movrit.movrit.participation.service;

import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.participation.entity.MemberCancelCount;
import com.movrit.movrit.participation.entity.MemberPenalty;
import com.movrit.movrit.participation.repository.MemberCancelCountRepository;
import com.movrit.movrit.participation.repository.MemberPenaltyRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참여자 취소 횟수를 남기고, 쌓이면 패널티로 바꿉니다.
 *
 * <ul>
 *   <li>취소 횟수는 1년(365일)이 지나면 세지 않습니다.
 *   <li>유효한 취소 횟수가 5회가 되면 패널티(CANCEL_LIMIT) 1회를 남기고, 그 5회는 다시 세지 않습니다.
 *   <li>패널티는 기존 규칙(90일 안 2회면 30일 신청 제한)에 그대로 들어갑니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CancelCountService {
  /** 취소 횟수가 유효한 기간(일)입니다. */
  public static final int VALID_DAYS = 365;

  /** 패널티로 바뀌는 취소 횟수입니다. */
  public static final int PENALTY_EVERY = 5;

  private final MemberCancelCountRepository countRepository;
  private final MemberPenaltyRepository penaltyRepository;
  private final NotificationService notificationService;

  /**
   * 취소 횟수 1회를 남깁니다. 같은 신청에는 한 번만 남기며, 5회가 쌓이면 패널티로 바꾸고 참여자에게 알립니다.
   *
   * @return 패널티로 바뀌었으면 true
   */
  @Transactional
  public boolean record(Long memberId, Long applicationId, String reasonType) {
    if (countRepository.existsByApplicationId(applicationId)) return false;
    countRepository.save(new MemberCancelCount(memberId, applicationId, reasonType));
    List<MemberCancelCount> counting = counting(memberId);
    if (counting.size() < PENALTY_EVERY) return false;
    // 자동으로 생기는 패널티라 기록자는 참여자 본인으로 남기고, 마지막 취소 신청에 붙입니다.
    MemberPenalty penalty =
        penaltyRepository.save(
            new MemberPenalty(
                memberId,
                applicationId,
                MemberPenalty.CANCEL_LIMIT,
                "1년 안에 취소 횟수가 " + PENALTY_EVERY + "회 쌓였습니다.",
                memberId));
    counting.stream().limit(PENALTY_EVERY).forEach(count -> count.convertTo(penalty.getId()));
    notificationService.send(
        memberId,
        "CAMPAIGN_PENALTY",
        "취소 횟수가 쌓여 패널티 1회가 기록되었습니다.",
        "1년 안의 취소 " + PENALTY_EVERY + "회가 패널티 1회로 바뀌었습니다.",
        "CAMPAIGN_APPLICATION",
        applicationId);
    return true;
  }

  /** 지금 세는 취소 횟수입니다(1년 안, 패널티로 바뀌지 않은 것). */
  @Transactional(readOnly = true)
  public long activeCount(Long memberId) {
    return counting(memberId).size();
  }

  private List<MemberCancelCount> counting(Long memberId) {
    LocalDateTime now = LocalDateTime.now();
    return countRepository.findAllByMemberIdOrderByCreatedAtAsc(memberId).stream()
        .filter(count -> count.isCounting(now, VALID_DAYS))
        .toList();
  }
}
