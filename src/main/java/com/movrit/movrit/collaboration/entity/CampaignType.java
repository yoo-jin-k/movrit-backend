package com.movrit.movrit.collaboration.entity;

/** 캠페인 유형입니다. 사용자 화면의 탐색 메뉴(제품·지역·기자단)와 1:1로 대응합니다. */
public enum CampaignType {
  /** 제품을 배송받아 체험하는 캠페인(메뉴: 제품) */
  PRODUCT,
  /** 매장·장소를 방문해 체험하는 캠페인(메뉴: 지역) */
  LOCAL,
  /** 제품 체험 없이 원고·소개 콘텐츠를 작성하는 캠페인(메뉴: 기자단) */
  REPORTER
}
