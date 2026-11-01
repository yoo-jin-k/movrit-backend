package com.movrit.movrit.channelconnect.entity;

/** 연결한 채널의 확인 상태입니다. */
public enum ChannelVerificationStatus {
  /** 직접 등록 후 운영자 확인을 기다립니다. */
  PENDING,
  /** 로그인 연결이거나 운영자가 확인했습니다. */
  VERIFIED,
  /** 운영자가 반려했습니다(사유를 함께 남깁니다). */
  REJECTED
}
