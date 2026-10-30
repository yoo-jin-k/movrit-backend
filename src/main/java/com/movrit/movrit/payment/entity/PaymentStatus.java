package com.movrit.movrit.payment.entity;

/** 결제 상태입니다. 결제 대기, 승인 완료, 실패, 취소, 환불이 있습니다. */
public enum PaymentStatus {
  READY,
  PAID,
  FAILED,
  CANCELLED,
  REFUNDED
}
