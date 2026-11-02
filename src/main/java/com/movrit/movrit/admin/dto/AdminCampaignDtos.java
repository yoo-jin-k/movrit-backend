package com.movrit.movrit.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 운영자 '캠페인 관리' 화면의 응답·요청 형식입니다. */
public final class AdminCampaignDtos {
  private AdminCampaignDtos() {}

  /**
   * 캠페인 한 줄입니다.
   *
   * @param state 표시 상태: UPCOMING(모집 예정)·RECRUITING(모집 중)·ENDED(모집 마감)·HIDDEN(숨김·마감 처리)
   * @param applied 취소를 뺀 신청 수
   * @param selected 선정(참여 중·제출·완료 포함) 수
   * @param completed 완료 수
   */
  public record Item(
      Long id,
      String title,
      String brandName,
      String campaignType,
      String selectionMode,
      String state,
      boolean open,
      LocalDateTime startAt,
      LocalDateTime endAt,
      Integer recruitmentCount,
      long applied,
      long selected,
      long completed,
      LocalDateTime createdAt) {}

  /** 숨김(마감) 사유입니다. 업체 알림과 감사 기록에 남습니다. */
  public record CloseRequest(@NotBlank @Size(max = 300) String reason) {}
}
