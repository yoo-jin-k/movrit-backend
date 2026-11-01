package com.movrit.movrit.channelconnect.service;

import com.movrit.movrit.channelconnect.entity.ChannelProvider;

/**
 * SNS 서비스와 실제로 통신하는 부분입니다. 테스트에서 가짜 구현으로 바꿔 끼울 수 있도록 인터페이스로 분리했습니다.
 *
 * <p>인가 코드를 접근 토큰으로 바꾸고, 그 토큰으로 계정 정보를 한 번 읽은 뒤 토큰은 버립니다.
 */
public interface ChannelOAuthGateway {

  /** SNS 로그인·동의 화면 주소를 만듭니다. */
  String authorizeUrl(ChannelProvider provider, String state);

  /** 인가 코드로 계정 정보를 가져옵니다. SNS가 거절하거나 응답이 이상하면 {@link ChannelGatewayException}을 던집니다. */
  ConnectedAccount fetchAccount(ChannelProvider provider, String code, String state);

  /**
   * SNS에서 받은 계정 정보입니다.
   *
   * @param externalId 계정 고유 번호
   * @param accountName 화면 표시 이름
   * @param profileUrl 채널 주소
   * @param followerCount 팔로워·구독자 수. 알 수 없으면 null
   */
  record ConnectedAccount(
      String externalId, String accountName, String profileUrl, Long followerCount) {}

  /** SNS와의 통신이 실패했을 때 화면에 보여줄 안내를 담는 예외입니다. */
  class ChannelGatewayException extends RuntimeException {
    public ChannelGatewayException(String message) {
      super(message);
    }

    public ChannelGatewayException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
