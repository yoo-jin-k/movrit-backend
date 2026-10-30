package com.movrit.movrit.preorder.entity;

/** 프리오더 진행 상태입니다. 초안, 판매 예약, 판매 중, 마감, 성공, 실패, 생산, 배송, 완료, 취소가 있습니다. */
public enum PreorderStatus {
  DRAFT,
  SCHEDULED,
  OPEN,
  CLOSED,
  SUCCESS,
  FAILED,
  PRODUCTION,
  SHIPPING,
  COMPLETED,
  CANCELLED
}
