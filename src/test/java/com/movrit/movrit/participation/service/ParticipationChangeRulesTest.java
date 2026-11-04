package com.movrit.movrit.participation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.participation.entity.CampaignExtensionRequest;
import com.movrit.movrit.participation.entity.CampaignPeriodExtension;
import com.movrit.movrit.participation.entity.MemberCancelCount;
import com.movrit.movrit.participation.entity.MemberPenalty;
import com.movrit.movrit.participation.repository.CampaignPeriodExtensionRepository;
import com.movrit.movrit.participation.repository.MemberCancelCountRepository;
import com.movrit.movrit.participation.repository.MemberPenaltyRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 참여 조정(신청 철회·참여 취소·기간 연장·취소 횟수)의 규칙을 확인합니다. */
class ParticipationChangeRulesTest {

  @Test
  void withdrawOnlyBeforeAnnouncement() {
    CampaignApplication application =
        application(CampaignApplicationStatus.SUBMITTED, LocalDateTime.now().plusDays(1));
    assertThat(ParticipationChangeService.canWithdraw(application, LocalDateTime.now())).isTrue();
    // 선정 발표 시각이 지나면 철회할 수 없습니다.
    assertThat(ParticipationChangeService.canWithdraw(application, LocalDateTime.now().plusDays(2)))
        .isFalse();
    application.withdraw("일정이 맞지 않습니다.");
    assertThat(application.getStatus()).isEqualTo(CampaignApplicationStatus.CANCELLED);
    assertThat(application.getCancellationReason()).startsWith("[신청 철회]");
    // 선정된 뒤에는 철회가 아니라 참여 취소를 써야 합니다.
    CampaignApplication selected =
        application(CampaignApplicationStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    assertThatThrownBy(() -> selected.withdraw("사정")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void participantCancelOnlyBeforeSubmission() {
    CampaignApplication active = application(CampaignApplicationStatus.ACTIVE, null);
    active.cancelForParticipant("참여자 취소", "개인 사정");
    assertThat(active.getStatus()).isEqualTo(CampaignApplicationStatus.CANCELLED);
    assertThat(active.getCancellationReason()).isEqualTo("[참여자 취소] 개인 사정");
    CampaignApplication submitted = application(CampaignApplicationStatus.CONTENT_SUBMITTED, null);
    assertThatThrownBy(() -> submitted.cancelForParticipant("참여자 취소", "사정"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void extensionMovesThisApplicationsExperienceAndDeadline() {
    CampaignApplication application = application(CampaignApplicationStatus.ACTIVE, null);
    LocalDateTime campaignDeadline = application.getCampaign().getContentSubmissionEndAt();
    LocalDateTime campaignExperienceEnd = application.getCampaign().getExperienceEndAt();
    assertThat(application.contentDeadline()).isEqualTo(campaignDeadline);
    assertThat(ParticipationChangeService.canRequestExtension(application, LocalDateTime.now()))
        .isTrue();

    CampaignExtensionRequest request =
        new CampaignExtensionRequest(1L, 7L, 7, "촬영 일정 조율", application.contentDeadline());
    request.decide(true, 2L, null);
    application.extendPeriod(request.getDays(), request.extendedDeadline());
    // 체험 기간과 콘텐츠 마감이 함께 7일 늘어납니다.
    assertThat(application.contentDeadline()).isEqualTo(campaignDeadline.plusDays(7));
    assertThat(application.experienceEnd()).isEqualTo(campaignExperienceEnd.plusDays(7));
    // 캠페인 일정 자체는 그대로입니다.
    assertThat(application.getCampaign().getContentSubmissionEndAt()).isEqualTo(campaignDeadline);
    assertThat(application.getCampaign().getExperienceEndAt()).isEqualTo(campaignExperienceEnd);
    // 한 번 처리한 요청은 다시 처리할 수 없고, 마감보다 앞당길 수도 없습니다.
    assertThatThrownBy(() -> request.decide(false, 2L, "중복"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> application.extendContentDeadline(campaignDeadline))
        .isInstanceOf(IllegalStateException.class);
    // 연장은 7일·14일만 됩니다.
    assertThatThrownBy(() -> new CampaignExtensionRequest(1L, 7L, 3, "사유", campaignDeadline))
        .isInstanceOf(IllegalArgumentException.class);
    // 마감이 지난 뒤에는 연장을 요청할 수 없습니다.
    assertThat(
            ParticipationChangeService.canRequestExtension(
                application, application.contentDeadline().plusMinutes(1)))
        .isFalse();
  }

  @Test
  void directExtensionHasNoCountLimitButRespectsState() {
    CampaignApplication application = application(CampaignApplicationStatus.ACTIVE, null);
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime deadline = application.contentDeadline();
    // 업체·운영자 직접 연장은 몇 번이든 됩니다(두 번 연속 늘려도 막지 않습니다).
    assertThat(ParticipationChangeService.directExtensionBlock(application, false, 3, now))
        .isNull();
    application.extendPeriod(3, deadline.plusDays(3));
    assertThat(ParticipationChangeService.directExtensionBlock(application, false, 5, now))
        .isNull();
    application.extendPeriod(5, application.contentDeadline().plusDays(5));
    assertThat(application.contentDeadline()).isEqualTo(deadline.plusDays(8));
    // 확인 중인 참여자 요청이 있으면 먼저 처리해야 합니다.
    assertThat(ParticipationChangeService.directExtensionBlock(application, true, 3, now))
        .contains("먼저");
    // 늘려도 마감이 이미 지난 시각이면 막습니다.
    assertThat(
            ParticipationChangeService.directExtensionBlock(
                application, false, 1, application.contentDeadline().plusDays(2)))
        .contains("지난");
    // 콘텐츠를 낸 뒤나 취소된 참여는 늘릴 수 없습니다.
    CampaignApplication cancelled = application(CampaignApplicationStatus.CANCELLED, null);
    assertThat(ParticipationChangeService.directExtensionBlock(cancelled, false, 3, now))
        .contains("진행 중");
    // 기록 출처: 참여자 요청 승인은 직접 연장으로 세지 않습니다.
    assertThat(
            new CampaignPeriodExtension(
                    1L,
                    7L,
                    7,
                    "사유",
                    CampaignPeriodExtension.PARTICIPANT_REQUEST,
                    2L,
                    null,
                    null,
                    deadline,
                    deadline.plusDays(7))
                .isDirect())
        .isFalse();
    assertThat(
            new CampaignPeriodExtension(
                    1L,
                    7L,
                    3,
                    "사유",
                    CampaignPeriodExtension.SELLER,
                    2L,
                    null,
                    null,
                    deadline,
                    deadline.plusDays(3))
                .isDirect())
        .isTrue();
  }

  @Test
  void cancelCountExpiresAfterOneYear() {
    MemberCancelCount recent = new MemberCancelCount(7L, 1L, MemberCancelCount.SELF_CANCEL);
    MemberCancelCount old = new MemberCancelCount(7L, 2L, MemberCancelCount.SELF_CANCEL);
    ReflectionTestUtils.setField(old, "createdAt", LocalDateTime.now().minusDays(400));
    LocalDateTime now = LocalDateTime.now();
    assertThat(recent.isCounting(now, CancelCountService.VALID_DAYS)).isTrue();
    assertThat(old.isCounting(now, CancelCountService.VALID_DAYS)).isFalse();
    recent.convertTo(99L);
    assertThat(recent.isCounting(now, CancelCountService.VALID_DAYS)).isFalse();
  }

  @Test
  void fiveCancelsBecomeOnePenalty() {
    MemberCancelCountRepository counts = mock(MemberCancelCountRepository.class);
    MemberPenaltyRepository penalties = mock(MemberPenaltyRepository.class);
    NotificationService notifications = mock(NotificationService.class);
    List<MemberCancelCount> saved = new ArrayList<>();
    when(counts.save(any()))
        .thenAnswer(
            invocation -> {
              saved.add(invocation.getArgument(0));
              return invocation.getArgument(0);
            });
    when(counts.findAllByMemberIdOrderByCreatedAtAsc(7L)).thenAnswer(invocation -> saved);
    when(penalties.save(any()))
        .thenAnswer(
            invocation -> {
              MemberPenalty penalty = invocation.getArgument(0);
              ReflectionTestUtils.setField(penalty, "id", 500L);
              return penalty;
            });
    CancelCountService service = new CancelCountService(counts, penalties, notifications);

    for (long applicationId = 1; applicationId <= 4; applicationId++) {
      assertThat(service.record(7L, applicationId, MemberCancelCount.SELF_CANCEL)).isFalse();
    }
    verify(penalties, never()).save(any());
    assertThat(service.activeCount(7L)).isEqualTo(4);

    assertThat(service.record(7L, 5L, MemberCancelCount.MISSED_DEADLINE)).isTrue();
    verify(penalties).save(any(MemberPenalty.class));
    verify(notifications)
        .send(eq(7L), eq("CAMPAIGN_PENALTY"), anyString(), anyString(), anyString(), anyLong());
    // 패널티로 바뀐 5회는 다시 세지 않습니다.
    assertThat(service.activeCount(7L)).isZero();
  }

  @Test
  void extensionCountWarnsAtFiveAndLimitsAtTen() {
    CampaignPeriodExtensionRepository logs = mock(CampaignPeriodExtensionRepository.class);
    MemberPenaltyRepository penalties = mock(MemberPenaltyRepository.class);
    MemberRepository members = mock(MemberRepository.class);
    NotificationService notifications = mock(NotificationService.class);
    List<CampaignPeriodExtension> saved = new ArrayList<>();
    when(logs.findAllByMemberIdOrderByCreatedAtAsc(7L)).thenAnswer(invocation -> saved);
    when(members.findAllByRoleAndStatus(any(), any())).thenReturn(List.of());
    when(penalties.save(any()))
        .thenAnswer(
            invocation -> {
              MemberPenalty penalty = invocation.getArgument(0);
              ReflectionTestUtils.setField(penalty, "id", 700L);
              return penalty;
            });
    ExtensionCountService service =
        new ExtensionCountService(logs, penalties, members, notifications);
    LocalDateTime deadline = LocalDateTime.now().plusDays(10);
    List<ExtensionCountService.Outcome> outcomes = new ArrayList<>();
    for (long i = 1; i <= 10; i++) {
      saved.add(
          new CampaignPeriodExtension(
              i,
              7L,
              3,
              "사유",
              CampaignPeriodExtension.SELLER,
              2L,
              null,
              null,
              deadline,
              deadline.plusDays(3)));
      outcomes.add(service.afterExtension(7L, i, "캠페인"));
    }
    // 5회째에 알리고, 10회째에 패널티(신청 제한)로 바뀝니다. 나머지는 아무 일도 없습니다.
    assertThat(outcomes.get(4)).isEqualTo(ExtensionCountService.Outcome.WARNED);
    assertThat(outcomes.get(9)).isEqualTo(ExtensionCountService.Outcome.LIMITED);
    assertThat(outcomes.stream().filter(o -> o == ExtensionCountService.Outcome.NONE).count())
        .isEqualTo(8);
    verify(penalties).save(any(MemberPenalty.class));
    // 패널티로 바뀐 10회는 다시 세지 않습니다.
    assertThat(service.countingCount(7L)).isZero();

    // 연장 누적 패널티 하나만으로 30일 동안 신청을 막고, 운영자가 취소하면 풀립니다.
    MemberPenalty limit = new MemberPenalty(7L, 10L, MemberPenalty.EXTENSION_LIMIT, "연장 10회", 7L);
    LocalDateTime until = ActivityService.restrictedUntil(List.of(limit));
    assertThat(until).isNotNull();
    assertThat(until).isAfter(LocalDateTime.now().plusDays(29));
    limit.revoke(1L, "확인 후 해제");
    assertThat(ActivityService.restrictedUntil(List.of(limit))).isNull();
    // 1년이 지난 연장은 세지 않습니다.
    CampaignPeriodExtension old =
        new CampaignPeriodExtension(
            1L,
            7L,
            3,
            "사유",
            CampaignPeriodExtension.SELLER,
            2L,
            null,
            null,
            deadline,
            deadline.plusDays(3));
    ReflectionTestUtils.setField(old, "createdAt", LocalDateTime.now().minusDays(400));
    assertThat(old.isCounting(LocalDateTime.now(), ExtensionCountService.VALID_DAYS)).isFalse();
  }

  private static CampaignApplication application(
      CampaignApplicationStatus status, LocalDateTime selectionAnnouncedAt) {
    Campaign campaign =
        Campaign.builder()
            .title("점검 캠페인")
            .recruitmentCount(3)
            .startAt(LocalDateTime.now().minusDays(3))
            .endAt(LocalDateTime.now().plusDays(1))
            .open(true)
            .build();
    ReflectionTestUtils.setField(campaign, "selectionAnnouncedAt", selectionAnnouncedAt);
    ReflectionTestUtils.setField(
        campaign, "contentSubmissionEndAt", LocalDateTime.now().plusDays(10).withNano(0));
    ReflectionTestUtils.setField(
        campaign, "experienceEndAt", LocalDateTime.now().plusDays(6).withNano(0));
    return CampaignApplication.builder().campaign(campaign).status(status).build();
  }
}
