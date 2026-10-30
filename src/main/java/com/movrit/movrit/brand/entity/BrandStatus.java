package com.movrit.movrit.brand.entity;

/** 브랜드의 운영 상태입니다. 판매자가 삭제(비활성)한 브랜드를 목록과 신규 캠페인 연결에서 구분할 때 사용합니다. */
public enum BrandStatus {
  /** 사용 중인 브랜드입니다. */
  ACTIVE,
  /** 판매자가 사용을 중단한 브랜드입니다. */
  INACTIVE
}
