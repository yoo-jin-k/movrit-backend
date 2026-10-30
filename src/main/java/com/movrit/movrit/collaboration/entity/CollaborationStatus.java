package com.movrit.movrit.collaboration.entity;

/** 기존 협업(Collaboration)의 진행 상태입니다. */
public enum CollaborationStatus {
  /** 신청 또는 제안 후 상대의 응답을 기다리는 상태입니다. */
  PENDING,
  /** 승인된 상태입니다. 현재 승인 시에는 바로 ACTIVE로 바뀌며 기존 데이터 호환을 위해 남겨 둔 값입니다. */
  APPROVED,
  /** 거절된 상태입니다. */
  REJECTED,
  /** 협업이 진행 중이며 추천 링크를 사용할 수 있는 상태입니다. */
  ACTIVE,
  /** 진행을 마치고 종료된 상태입니다. */
  ENDED,
  /** 종료 전에 취소된 상태입니다. */
  CANCELLED
}
