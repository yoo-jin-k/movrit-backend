package com.movrit.movrit.chat.entity;

/** 채팅방 안에서의 역할입니다. 메시지에 함께 남겨 화면에 '브랜드'·'운영자' 같은 표시를 붙입니다. */
public enum ChatRole {
  /** 캠페인 신청자(참여자)입니다. */
  CREATOR,
  /** 캠페인을 운영하는 브랜드(업체)입니다. */
  BRAND,
  /** 운영자입니다. */
  ADMIN,
  /** 운영자 문의방을 연 회원입니다(참여자·브랜드 구분 없이). */
  MEMBER
}
