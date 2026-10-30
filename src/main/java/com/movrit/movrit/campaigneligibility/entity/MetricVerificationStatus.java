package com.movrit.movrit.campaigneligibility.entity;

/** 회원이 입력한 채널 지표의 확인 상태입니다. */
public enum MetricVerificationStatus {
  /** 아직 확인되지 않은 입력값입니다. 새로 입력하거나 수정하면 이 상태가 됩니다. */
  UNVERIFIED,
  /** 운영 확인을 거친 값입니다. 신청 조건 비교에 사용할 수 있습니다. */
  VERIFIED,
  /** 확인 결과 인정되지 않은 값입니다. */
  REJECTED
}
