package com.movrit.movrit.seller.entity;

/** 업체(판매자) 등록 심사 상태입니다. 승인된 업체만 브랜드·캠페인 운영 화면을 사용할 수 있습니다. */
public enum SellerStatus {
  /** 업체 신청 후 운영자 심사를 기다리는 상태입니다. */
  PENDING,
  /** 심사를 통과해 업체 기능을 사용할 수 있는 상태입니다. */
  APPROVED,
  /** 심사에서 반려된 상태입니다. 정보를 고쳐 다시 신청할 수 있습니다. */
  REJECTED,
  /** 운영자가 업체 권한을 정지한 상태입니다. */
  SUSPENDED
}
