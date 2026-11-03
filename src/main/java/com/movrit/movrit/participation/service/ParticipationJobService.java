package com.movrit.movrit.participation.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.participation.dto.ParticipationChangeDtos;
import com.movrit.movrit.participation.entity.CampaignCancelRequest;
import com.movrit.movrit.participation.entity.CampaignExtensionRequest;
import com.movrit.movrit.participation.entity.CampaignUnselectedRecord;
import com.movrit.movrit.participation.entity.MemberCancelCount;
import com.movrit.movrit.participation.repository.CampaignCancelRequestRepository;
import com.movrit.movrit.participation.repository.CampaignExtensionRequestRepository;
import com.movrit.movrit.participation.repository.CampaignUnselectedRecordRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 30분마다 도는 참여 운영 자동 처리입니다(마감 알림과 15분 어긋나게 돕니다).
 *
 * <ul>
 *   <li>마감 미제출 자동 취소: 콘텐츠 제출 마감(연장 반영)이 지났는데 아직 내지 않은 선정·진행 중 참여를 취소하고 취소 횟수 1회를 남깁니다. 기간 연장이나 협의
 *       취소를 확인 중인 참여는 결과가 나올 때까지 건드리지 않습니다.
 *   <li>업체 미선정 기록: 업체 직접 선정 캠페인에 신청자가 있었는데 선정 마감까지 한 명도 선정하지 않았으면 캠페인마다 한 번 기록하고 업체에 알립니다.
 * </ul>
 *
 * <p>기능을 켜기 전에 이미 오래 지난 신청·캠페인(보존 중인 점검·시연 데이터)은 바꾸지 않도록, 최근에 마감이 지난 것만 봅니다(제출 마감 3일, 선정 마감 7일).
 */
@Service
@RequiredArgsConstructor
public class ParticipationJobService {
  static final int MISSED_LOOKBACK_DAYS = 3;
  static final int UNSELECTED_LOOKBACK_DAYS = 7;
  private static final List<CampaignApplicationStatus> IN_PROGRESS =
      List.of(CampaignApplicationStatus.SELECTED, CampaignApplicationStatus.ACTIVE);

  private final CampaignApplicationRepository applicationRepository;
  private final CampaignRepository campaignRepository;
  private final CampaignExtensionRequestRepository extensionRepository;
  private final CampaignCancelRequestRepository cancelRequestRepository;
  private final CampaignUnselectedRecordRepository unselectedRepository;
  private final CancelCountService cancelCountService;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;
  private final MemberService memberService;

  @Scheduled(cron = "0 15,45 * * * *")
  public void scheduled() {
    run();
  }

  /** 운영자가 점검용으로 바로 실행합니다. */
  @Transactional
  public ParticipationChangeDtos.JobResult runNow(Long adminMemberId) {
    memberService.getAdmin(adminMemberId);
    return run();
  }

  /** 두 자동 처리를 차례로 실행하고 처리한 개수를 돌려줍니다. */
  @Transactional
  public ParticipationChangeDtos.JobResult run() {
    LocalDateTime now = LocalDateTime.now();
    return new ParticipationChangeDtos.JobResult(cancelMissedDeadlines(now), recordUnselected(now));
  }

  /** 제출 마감이 지난 참여를 자동으로 취소합니다. */
  int cancelMissedDeadlines(LocalDateTime now) {
    int cancelled = 0;
    for (CampaignApplication application :
        applicationRepository.findContentDeadlinePassed(
            IN_PROGRESS, now.minusDays(MISSED_LOOKBACK_DAYS), now)) {
      Long id = application.getId();
      // 연장·협의 취소를 확인 중이면 결과가 나올 때까지 기다립니다.
      if (extensionRepository.existsByApplicationIdAndStatus(id, CampaignExtensionRequest.REQUESTED)
          || cancelRequestRepository.existsByApplicationIdAndStatus(
              id, CampaignCancelRequest.REQUESTED)) continue;
      application.cancelForParticipant("제출 마감 지남", "콘텐츠 제출 마감까지 제출하지 않아 자동으로 취소되었습니다.");
      Long memberId = application.getCreator().getMember().getId();
      cancelCountService.record(memberId, id, MemberCancelCount.MISSED_DEADLINE);
      String title = application.getCampaign().getTitle();
      notificationService.send(
          memberId,
          "CAMPAIGN_DEADLINE",
          "콘텐츠 제출 마감이 지나 참여가 취소되었습니다.",
          title + " · 취소 횟수 1회가 남았습니다.",
          "CAMPAIGN_APPLICATION",
          id);
      notificationService.send(
          application.getCampaign().getBrand().getSeller().getMember().getId(),
          "CAMPAIGN_APPLICATION",
          "콘텐츠를 내지 않은 참여가 자동으로 취소되었습니다.",
          application.getCreator().getNickname() + " · " + title,
          "CAMPAIGN_APPLICATION",
          id);
      auditLogService.record(
          memberId,
          "CAMPAIGN_APPLICATION_DEADLINE_MISSED",
          "CAMPAIGN_APPLICATION",
          id,
          "{\"status\":\"ACTIVE\"}",
          "{\"status\":\"CANCELLED\"}",
          "콘텐츠 제출 마감 지남");
      cancelled += 1;
    }
    return cancelled;
  }

  /** 선정 마감까지 아무도 선정하지 않은 업체 직접 선정 캠페인을 기록합니다. */
  int recordUnselected(LocalDateTime now) {
    int recorded = 0;
    for (Campaign campaign :
        campaignRepository.findBrandSelectionDue(now.minusDays(UNSELECTED_LOOKBACK_DAYS), now)) {
      if (unselectedRepository.existsById(campaign.getId())) continue;
      long applications = applicationRepository.countByCampaignId(campaign.getId());
      if (applications == 0 || applicationRepository.countEverSelected(campaign.getId()) > 0)
        continue;
      Long sellerMemberId = campaign.getBrand().getSeller().getMember().getId();
      unselectedRepository.save(
          new CampaignUnselectedRecord(campaign.getId(), sellerMemberId, (int) applications));
      notificationService.send(
          sellerMemberId,
          "CAMPAIGN_UNSELECTED",
          "선정 마감까지 참여자를 선정하지 않아 미선정 1회가 기록되었습니다.",
          campaign.getTitle() + " · 신청 " + applications + "명",
          "CAMPAIGN",
          campaign.getId());
      recorded += 1;
    }
    return recorded;
  }
}
