package com.movrit.movrit.campaignapplication.entity;

/**
 * 캠페인 신청 한 건의 진행 단계입니다.
 *
 * <p>기본 흐름: 신청 접수 → 선정 → 참여 수락(진행 중) → 콘텐츠 제출 → (수정 요청) → 완료.
 */
public enum CampaignApplicationStatus {
  /** 신청서를 제출하고 선정 결과를 기다리는 상태입니다. */
  SUBMITTED,
  /** 선정되었고 신청자의 참여 수락을 기다리는 상태입니다. */
  SELECTED,
  /** 선정되지 않은 상태입니다. 더 이상 진행하지 않습니다. */
  REJECTED,
  /** 신청자가 참여를 수락해 체험과 콘텐츠 준비를 진행하는 상태입니다. */
  ACTIVE,
  /** 콘텐츠 링크를 제출하고 검토를 기다리는 상태입니다. */
  CONTENT_SUBMITTED,
  /** 브랜드 또는 운영자가 콘텐츠 수정을 요청한 상태입니다. 다시 제출할 수 있습니다. */
  REVISION_REQUESTED,
  /** 운영자 최종 승인까지 끝나 캠페인 참여가 완료된 상태입니다. */
  COMPLETED,
  /** 신청자 또는 관리 주체가 신청을 취소한 상태입니다. */
  CANCELLED
}
