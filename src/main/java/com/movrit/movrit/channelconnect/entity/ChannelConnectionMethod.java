package com.movrit.movrit.channelconnect.entity;

/** 채널을 연결한 방법입니다. */
public enum ChannelConnectionMethod {
  /** SNS 계정 로그인으로 연결했습니다. SNS가 알려준 값이라 바로 '확인됨'입니다. */
  OAUTH,
  /** 회원이 주소·수치·프로필 캡처를 직접 등록했습니다. 운영자 확인이 필요합니다. */
  MANUAL
}
