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
      LocalDateTime decidedAt) {}

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
}
