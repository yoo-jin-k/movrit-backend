package com.movrit.movrit.participation.dto;

import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** 참여 조정(신청 철회·참여 취소·협의 취소·기간 연장·업체 미선정) API의 요청·응답 형식입니다. */
public final class ParticipationChangeDtos {
  private ParticipationChangeDtos() {}

  /** 신청 철회 요청입니다(선정 발표 전). 사유는 선택입니다. */
  public record WithdrawRequest(@Size(max = 500) String reason) {}

  /** 참여 취소(단순 취소) 요청입니다. 취소 횟수 1회가 남습니다. */
  public record SelfCancelRequest(@NotBlank @Size(max = 500) String reason) {}

  /** 기간 연장 요청입니다. days는 7 또는 14입니다. */
  public record ExtensionRequest(@NotNull Integer days, @NotBlank @Size(max = 500) String reason) {}

  /** 승인·반려 결정입니다. 반려할 때는 note에 사유를 적습니다. */
  public record Decision(@NotNull Boolean approve, @Size(max = 500) String note) {}

  /**
   * 참여자 화면에서 이 신청으로 지금 할 수 있는 일과 진행 중인 요청입니다.
   *
   * @param canWithdraw 선정 발표 전이라 신청을 철회할 수 있는지
   * @param canCancel 선정·진행 중이라 참여 취소(단순·협의)를 할 수 있는지
   * @param canRequestExtension 기간 연장을 요청할 수 있는지(신청당 1회, 마감 전)
   * @param contentDeadline 이 신청의 콘텐츠 제출 마감(연장 반영)
   * @param extendedUntil 연장이 승인된 경우의 새 마감
   * @param experienceExtendedUntil 연장이 승인된 경우의 새 체험 종료(캠페인에 체험 종료가 없으면 비어 있음)
   * @param cancelCount 지금 세는 취소 횟수(1년 안)
   * @param cancelLimit 패널티로 바뀌는 취소 횟수
   */
  public record Status(
      Long applicationId,
      CampaignApplicationStatus status,
      boolean canWithdraw,
      boolean canCancel,
      boolean canRequestExtension,
      LocalDateTime contentDeadline,
      LocalDateTime extendedUntil,
      LocalDateTime experienceExtendedUntil,
      Extension extension,
      CancelRequest cancelRequest,
      long cancelCount,
      int cancelLimit) {}

  /** 기간 연장 요청 한 건입니다. */
  public record Extension(
      Long applicationId,
      Long campaignId,
      String campaignTitle,
      String participantNickname,
      int days,
      String reason,
      LocalDateTime previousDeadline,
      LocalDateTime extendedDeadline,
      String status,
      String decisionNote,
      LocalDateTime createdAt,
      LocalDateTime decidedAt,
      String brandName,
      Long participantMemberId) {}

  /** 협의 취소 요청 한 건입니다. 증빙 이미지는 별도 주소로 내려받습니다. */
  public record CancelRequest(
      Long id,
      Long applicationId,
      Long campaignId,
      String campaignTitle,
      String brandName,
      String participantNickname,
      String reason,
      String status,
      String decisionNote,
      LocalDateTime createdAt,
      LocalDateTime decidedAt) {}

  /** 업체 미선정 기록 한 건입니다. */
  public record Unselected(
      Long campaignId,
      String campaignTitle,
      String brandName,
      int applicationCount,
      LocalDateTime recordedAt) {}

  /** 업체 미선정 기록 목록과 횟수입니다. */
  public record UnselectedSummary(long count, List<Unselected> records) {}

  /** 예약 작업을 바로 실행한 결과입니다(운영자 점검용). */
  public record JobResult(int missedDeadlineCancelled, int unselectedRecorded) {}

  /** 업체·운영자가 참여자 기간을 직접 늘리는 요청입니다. days는 1~30일이고 사유는 참여자에게도 보입니다. */
  public record DirectExtensionRequest(
      @NotNull Integer days, @NotBlank @Size(max = 500) String reason) {}

  /**
   * 기간 연장 기록 한 건입니다.
   *
   * @param source PARTICIPANT_REQUEST(참여자 요청 승인)·SELLER(업체 직접)·ADMIN(운영자 직접)
   */
  public record ExtensionLog(
      Long id,
      Long applicationId,
      Long campaignId,
      String campaignTitle,
      String brandName,
      int days,
      String reason,
      String source,
      String extendedByNickname,
      LocalDateTime previousExperienceEnd,
      LocalDateTime newExperienceEnd,
      LocalDateTime previousDeadline,
      LocalDateTime newDeadline,
      LocalDateTime createdAt) {}

  /**
   * 신청 하나의 연장 현황입니다(업체·운영자 참여자 상세).
   *
   * @param canExtend 지금 직접 연장할 수 있는지(콘텐츠 제출 전 진행 중, 확인 중인 참여자 요청 없음)
   * @param participantYearCount 이 참여자가 최근 1년 동안 받은 연장 횟수(모든 캠페인)
   * @param countingCount 횟수 기준으로 지금 세는 연장 수(1년 안, 패널티로 바뀌지 않은 것)
   * @param warnAt 운영자에게 알리는 횟수(5)
   * @param limitAt 패널티·신청 제한으로 바뀌는 횟수(10)
   */
  public record ApplicationExtensions(
      Long applicationId,
      LocalDateTime experienceEnd,
      LocalDateTime contentDeadline,
      boolean canExtend,
      boolean pendingRequest,
      long participantYearCount,
      long participantTotalCount,
      List<ExtensionLog> logs,
      long countingCount,
      int warnAt,
      int limitAt) {}

  /**
   * 회원 한 명의 연장 이력입니다(운영자 회원 상세).
   *
   * @param yearCount 최근 1년 연장 횟수
   * @param requestCount 참여자 요청으로 승인된 횟수
   * @param directCount 업체·운영자가 직접 늘린 횟수
   * @param rejectedRequestCount 반려된 참여자 요청 수
   * @param countingCount 횟수 기준으로 지금 세는 연장 수(1년 안, 패널티로 바뀌지 않은 것)
   * @param limitPenaltyCount 지금까지 기록된 연장 누적 패널티 수(취소한 것 포함)
   * @param restrictedUntil 신청 제한이 끝나는 시각(제한 중이 아니면 null)
   */
  public record MemberExtensionHistory(
      Long memberId,
      long totalCount,
      long yearCount,
      long requestCount,
      long directCount,
      long rejectedRequestCount,
      List<ExtensionLog> logs,
      long countingCount,
      int warnAt,
      int limitAt,
      long limitPenaltyCount,
      LocalDateTime restrictedUntil) {}
}
