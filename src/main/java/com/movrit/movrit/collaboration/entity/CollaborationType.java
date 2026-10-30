package com.movrit.movrit.collaboration.entity;

/** 기존 협업이 어떻게 시작되었는지 구분합니다. */
public enum CollaborationType {
  /** 크리에이터가 먼저 신청한 협업입니다. */
  APPLICATION,
  /** 업체가 크리에이터에게 먼저 제안한 협업입니다. */
  PROPOSAL
}
