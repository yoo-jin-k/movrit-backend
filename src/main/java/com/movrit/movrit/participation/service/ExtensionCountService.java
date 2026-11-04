package com.movrit.movrit.participation.service;

import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.participation.entity.CampaignPeriodExtension;
import com.movrit.movrit.participation.entity.MemberPenalty;
import com.movrit.movrit.participation.repository.CampaignPeriodExtensionRepository;
import com.movrit.movrit.participation.repository.MemberPenaltyRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기간 연장 횟수 기준을 적용합니다. 참여자 요청 승인과 업체·운영자 직접 연장을 모두 셉니다.
 *
 * <ul>
 *   <li>연장 기록은 1년(365일)이 지나면 세지 않습니다.
 *   <li>세는 연장이 5회가 되면 운영자 전원에게 알리고(회원 상세로 이동), 참여자에게도 10회 기준을 미리 알립니다.
 *   <li>세는 연장이 10회가 되면 패널티(EXTENSION_LIMIT) 1회를 남기고 30일 동안 새 캠페인 신청을 막습니다. 센 연장은 다시 세지 않고 0회부터 셉니다.
 *   <li>운영자가 패널티를 취소하면 신청 제한도 풀립니다(패널티 관리 화면).
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ExtensionCountService {
  /** 연장 기록을 세는 기간(일)입니다. */
  public static final int VALID_DAYS = 365;

  /** 운영자에게 알리는 횟수입니다. */
  public static final int WARN_AT = 5;

  /** 패널티와 신청 제한으로 바뀌는 횟수입니다. */
  public static final int LIMIT_AT = 10;

  /** 연장 누적 패널티로 신청을 막는 기간(일)입니다. */
  public static final int RESTRICT_DAYS = 30;

  private final CampaignPeriodExtensionRepository extensionRepository;
  private final MemberPenaltyRepository penaltyRepository;
  private final MemberRepository memberRepository;
  private final NotificationService notificationService;

  /** 결과: 운영자 알림을 보냈는지, 패널티(신청 제한)로 바뀌었는지입니다. */
  public enum Outcome {
    NONE,
    WARNED,
    LIMITED
  }

  /**
   * 연장 기록 한 줄을 남긴 뒤 부릅니다. 세는 횟수가 5회에 닿으면 알리고, 10회가 되면 패널티로 바꿉니다.
   *
   * @param memberId 연장을 받은 참여자
   * @param applicationId 방금 연장한 신청(패널티는 이 신청에 붙습니다)
   */
  @Transactional
  public Outcome afterExtension(Long memberId, Long applicationId, String campaignTitle) {
    List<CampaignPeriodExtension> counting = counting(memberId);
    int count = counting.size();
    if (count >= LIMIT_AT) {
      MemberPenalty penalty =
          penaltyRepository.save(
              new MemberPenalty(
                  memberId,
                  applicationId,
                  MemberPenalty.EXTENSION_LIMIT,
                  "1년 안에 기간 연장이 " + LIMIT_AT + "회 쌓였습니다.",
                  memberId));
      // 세던 연장은 모두 패널티로 바꾸고 0회부터 다시 셉니다(기준 도입 전에 이미 10회를 넘긴 경우 포함).
      counting.forEach(log -> log.convertTo(penalty.getId()));
      notificationService.send(
          memberId,
          "CAMPAIGN_PENALTY",
          "기간 연장이 " + LIMIT_AT + "회 쌓여 패널티 1회가 기록되었습니다.",
          RESTRICT_DAYS + "일 동안 새 캠페인에 신청할 수 없습니다. · " + campaignTitle,
          "CAMPAIGN_APPLICATION",
          applicationId);
      notifyAdmins(memberId, "기간 연장 " + LIMIT_AT + "회로 신청 제한이 걸린 참여자가 있습니다.");
      return Outcome.LIMITED;
    }
    if (count == WARN_AT) {
      notificationService.send(
          memberId,
          "CAMPAIGN_EXTENSION",
          "기간 연장이 1년 안에 " + WARN_AT + "회가 되었습니다.",
          LIMIT_AT + "회가 되면 패널티 1회가 기록되고 " + RESTRICT_DAYS + "일 동안 새 캠페인에 신청할 수 없습니다.",
          "CAMPAIGN_APPLICATION",
          applicationId);
      notifyAdmins(memberId, "기간 연장이 1년 안에 " + WARN_AT + "회인 참여자가 있습니다.");
      return Outcome.WARNED;
    }
    return Outcome.NONE;
  }

  /** 지금 세는 연장 횟수입니다(1년 안, 패널티로 바뀌지 않은 것). */
  @Transactional(readOnly = true)
  public long countingCount(Long memberId) {
    return counting(memberId).size();
  }

  private List<CampaignPeriodExtension> counting(Long memberId) {
    LocalDateTime now = LocalDateTime.now();
    return extensionRepository.findAllByMemberIdOrderByCreatedAtAsc(memberId).stream()
        .filter(log -> log.isCounting(now, VALID_DAYS))
        .toList();
  }

  private void notifyAdmins(Long memberId, String title) {
    String nickname =
        memberRepository.findById(memberId).map(member -> member.getNickname()).orElse("참여자");
    for (var admin :
        memberRepository.findAllByRoleAndStatus(MemberRole.ADMIN, MemberStatus.ACTIVE)) {
      // 운영자 알림은 그 참여자의 회원 상세(연장 이력)로 이동합니다.
      notificationService.send(
          admin.getId(), "CAMPAIGN_EXTENSION", title, nickname, "ADMIN_MEMBER", memberId);
    }
  }
}
