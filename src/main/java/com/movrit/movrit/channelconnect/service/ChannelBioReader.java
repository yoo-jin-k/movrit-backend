package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.channelconnect.entity.ChannelProvider;

/** SNS 공개 정보에서 소개글(과 팔로워 수)을 읽습니다. 소개글 인증 코드 확인에 씁니다. */
public interface ChannelBioReader {

  /** 이 서비스를 지금 자동으로 확인할 수 있는지 알려줍니다(인스타그램은 서버에 조회 키가 있어야 합니다). */
  boolean autoCheck(ChannelProvider provider);

  /**
   * 소개글을 읽습니다. 계정 아이디는 서비스 형식 검사를 마친 값만 넘깁니다.
   *
   * @param accountId 네이버 블로그 아이디 또는 인스타그램 사용자 이름
   */
  BioReadResult read(ChannelProvider provider, String accountId);

  /** 읽은 결과입니다. */
  enum Status {
    /** 소개글을 읽었습니다. */
    FOUND,
    /** 그런 계정(블로그)이 없습니다. */
    NOT_FOUND,
    /** 지금 자동으로 확인할 수 없습니다(조회 키 없음, 개인 계정, 통신 실패 등). */
    UNAVAILABLE
  }

  /**
   * @param bio 소개글(FOUND일 때만)
   * @param followerCount 팔로워 수. 서비스가 알려주지 않으면 null
   * @param accountName 화면에 보여줄 이름(블로그 제목 등). 없으면 null
   */
  record BioReadResult(Status status, String bio, Long followerCount, String accountName) {
    public static BioReadResult found(String bio, Long followerCount, String accountName) {
      return new BioReadResult(Status.FOUND, bio, followerCount, accountName);
    }

    public static BioReadResult notFound() {
      return new BioReadResult(Status.NOT_FOUND, null, null, null);
    }

    public static BioReadResult unavailable() {
      return new BioReadResult(Status.UNAVAILABLE, null, null, null);
    }
  }
}
