package com.movrit.movrit.order.entity;

/** 주문 진행 상태입니다. 결제 대기, 결제 완료, 배송 준비, 발송, 배송 완료, 취소, 환불이 있습니다. */
public enum OrderStatus {
  PENDING_PAYMENT,
  PAID,
  PREPARING,
  SHIPPED,
  DELIVERED,
  CANCELLED,
  REFUNDED
}
