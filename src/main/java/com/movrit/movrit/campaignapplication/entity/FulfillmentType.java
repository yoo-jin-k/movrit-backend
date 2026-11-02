package com.movrit.movrit.campaignapplication.entity;

/** 체험 방식입니다. 캠페인이 허용한 방식 중 하나를 신청자가 골라 신청합니다. */
public enum FulfillmentType {
  /** 배송형: 브랜드가 체험 상품을 신청자 주소로 보냅니다. */
  DELIVERY,
  /** 방문형: 신청자가 매장·장소에 직접 방문해 체험합니다. */
  VISIT,
  /** 구매형: 신청자가 구매처에서 직접 구매합니다. 배송 주소를 받지 않습니다(구매평 캠페인). */
  PURCHASE
}
