package com.movrit.movrit.campaigncontent.entity;

/** 콘텐츠가 게시된 외부 채널을 구분합니다. 서버가 외부 게시물을 자동 수집하지는 않습니다. */
public enum ContentChannel {
  /** 인스타그램 게시물·릴스입니다. 게시물 주소 형식을 따로 검사합니다. */
  INSTAGRAM,
  /** 블로그 글입니다. */
  BLOG,
  /** 유튜브 영상입니다. */
  YOUTUBE,
  /** 네이버 클립 등 짧은 영상입니다. */
  CLIP,
  /** 위에 없는 기타 공개 채널입니다. */
  OTHER
}
