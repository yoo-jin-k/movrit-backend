package com.movrit.movrit.campaigncontent.entity;

/**
 * 제출된 콘텐츠의 검토 단계입니다.
 *
 * <p>브랜드 확인과 운영자 최종 승인을 나누어, 브랜드가 확인해도 운영자 승인 전에는 캠페인이 완료되지 않습니다.
 */
public enum ContentSubmissionStatus {
  /** 제출(또는 재제출)되어 검토를 기다리는 상태입니다. */
  SUBMITTED,
  /** 브랜드나 운영자가 수정을 요청한 상태입니다. 참여자가 다시 제출할 수 있습니다. */
  REVISION_REQUESTED,
  /** 브랜드가 확인을 마치고 운영자 최종 승인을 기다리는 상태입니다. */
  BRAND_APPROVED,
  /** 운영자가 최종 승인한 상태입니다. 신청도 완료로 바뀝니다. */
  APPROVED
}
