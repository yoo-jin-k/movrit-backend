package com.movrit.movrit.settlement.entity;

/** 정산 상태입니다. 정산 대기, 확정, 지급 완료, 취소가 있습니다. */
public enum SettlementStatus {
  PENDING,
  CONFIRMED,
  PAID,
  CANCELLED
}
