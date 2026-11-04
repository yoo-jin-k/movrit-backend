package com.movrit.movrit.channelconnect.entity;

/** 채널을 연결한 방법입니다. */
public enum ChannelConnectionMethod {
  /** SNS 계정 로그인으로 연결했습니다. SNS가 알려준 값이라 바로 '확인됨'입니다. */
  OAUTH,
  /** 회원이 주소·수치·프로필 캡처를 직접 등록했습니다. 운영자 확인이 필요합니다. */
  MANUAL,
  /**
   * 회원이 받은 인증 코드를 소개글에 넣고, 서버가 공개 정보(네이버 블로그 RSS·인스타그램 Business Discovery)에서 코드를 확인했습니다. 자동으로 확인할 수
   * 없는 인스타그램 계정은 '확인 대기'로 두고 운영자가 소개글의 코드를 봅니다.
   */
  BIO_CODE
}
