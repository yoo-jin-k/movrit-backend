package com.movrit.movrit.chat.entity;

/** 채팅방 종류입니다. */
public enum ChatRoomType {
  /** 캠페인 신청 한 건에 대한 참여자 ↔ 브랜드 대화입니다. 운영자도 열람·참여할 수 있습니다. */
  APPLICATION,
  /** 회원(참여자·브랜드) ↔ 운영자 1:1 문의입니다. */
  SUPPORT
}
