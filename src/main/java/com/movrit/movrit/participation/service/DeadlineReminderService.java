package com.movrit.movrit.participation.service;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.participation.entity.DeadlineReminder;
import com.movrit.movrit.participation.repository.DeadlineReminderRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콘텐츠 제출 마감 자동 알림(D-3·D-1)을 보냅니다.
 *
 * <ul>
 *   <li>대상: 진행 중(또는 수정 요청)이고 아직 콘텐츠를 내지 않은 신청 중, 캠페인 콘텐츠 마감까지 3일 이내인 것
 *   <li>마감 24시간 이내면 D-1, 72시간 이내면 D-3 알림을 보냅니다. 같은 신청·단계는 한 번만 보냅니다(D-1을 먼저 보냈다면 D-3은 보내지 않습니다).
 *   <li>30분마다 확인합니다. 운영자는 시연·점검용으로 바로 실행할 수 있습니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DeadlineReminderService {
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
  private static final List<CampaignApplicationStatus> WAITING =
      List.of(CampaignApplicationStatus.ACTIVE, CampaignApplicationStatus.REVISION_REQUESTED);

  private final CampaignApplicationRepository applicationRepository;
  private final DeadlineReminderRepository reminderRepository;
  private final NotificationService notificationService;

  @Scheduled(cron = "0 */30 * * * *")
  public void scheduled() {
    run();
  }

  /** 지금 보낼 알림을 모두 보내고 보낸 개수를 돌려줍니다. */
  @Transactional
  public int run() {
    LocalDateTime now = LocalDateTime.now();
    int sent = 0;
    for (CampaignApplication application :
        applicationRepository.findContentDeadlineDue(WAITING, now, now.plusDays(3))) {
      LocalDateTime deadline = application.getCampaign().getContentSubmissionEndAt();
      long hours = Duration.between(now, deadline).toHours();
      String stage = hours < 24 ? "D1" : "D3";
      Long id = application.getId();
      if (reminderRepository.existsByApplicationIdAndStage(id, stage)) continue;
      // D-1을 보냈다면 뒤늦게 D-3을 보내지 않습니다.
      if (stage.equals("D3") && reminderRepository.existsByApplicationIdAndStage(id, "D1"))
        continue;
      reminderRepository.save(new DeadlineReminder(id, stage));
      if (stage.equals("D1") && !reminderRepository.existsByApplicationIdAndStage(id, "D3")) {
        reminderRepository.save(new DeadlineReminder(id, "D3"));
      }
      notificationService.send(
          application.getCreator().getMember().getId(),
          "CAMPAIGN_DEADLINE",
          (stage.equals("D1") ? "[D-1]" : "[D-3]") + " 콘텐츠 제출 마감이 다가옵니다.",
          application.getCampaign().getTitle() + " · " + deadline.format(TIME) + "까지 제출해 주세요.",
          "CAMPAIGN_APPLICATION",
          id);
      sent += 1;
    }
    return sent;
  }
}
