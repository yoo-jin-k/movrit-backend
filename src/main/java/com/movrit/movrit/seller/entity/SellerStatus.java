package com.movrit.movrit.seller.entity;

/** 판매자 등록 심사 상태. 승인된 판매자만 이후 브랜드 상품 등록과 프리오더 개설 권한을 갖도록 사용합니다. */
public enum SellerStatus {
  PENDING,
  APPROVED,
  REJECTED,
  SUSPENDED
}
