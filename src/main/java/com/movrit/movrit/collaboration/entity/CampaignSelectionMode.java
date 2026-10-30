package com.movrit.movrit.collaboration.entity;

/** 캠페인 신청자를 최종 선정할 주체를 구분합니다. */
public enum CampaignSelectionMode {
  /** 캠페인을 소유한 브랜드가 직접 참여자를 선정합니다. */
  BRAND_DIRECT,
  /** MOVRIT 운영자(관리자)가 참여자를 선정합니다. */
  OPERATOR
}
