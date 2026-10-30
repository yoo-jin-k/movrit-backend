package com.movrit.movrit.shipping.entity;

/** 배송 상태입니다. 배송 대기, 준비 중, 발송, 배송 완료, 반품이 있습니다. */
public enum ShippingStatus {
  READY,
  PREPARING,
  SHIPPED,
  DELIVERED,
  RETURNED
}
