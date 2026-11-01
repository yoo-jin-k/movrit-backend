package com.movrit.movrit.channelconnect.dto;

import com.movrit.movrit.channelconnect.entity.MemberChannelConnection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** SNS 채널 연결 화면과 주고받는 요청·응답 형식입니다. */
public final class ChannelConnectDtos {
  private ChannelConnectDtos() {}

  /**
   * 서비스 하나의 연결 상태입니다.
   *
   * @param provider 서비스 주소 이름(instagram·youtube·naver-blog)
   * @param label 화면 표시 이름
   * @param available 서버에 연동 키가 설정되어 있어 연결을 시작할 수 있는지
   * @param connection 연결된 계정 정보. 연결 전이면 null
   */
  public record ProviderStatus(
      String provider, String label, boolean available, Connection connection) {}

  /** 연결된 계정 정보입니다. 접근 토큰은 담지 않습니다. */
  public record Connection(
      String accountName, String profileUrl, Long followerCount, LocalDateTime connectedAt) {
    public static Connection from(MemberChannelConnection value) {
      return new Connection(
          value.getAccountName(),
          value.getProfileUrl(),
          value.getFollowerCount(),
          value.getConnectedAt());
    }
  }

  /** 연결 시작 응답입니다. 화면은 이 주소로 이동해 SNS 로그인·동의를 진행합니다. */
  public record StartResponse(String authorizeUrl) {}

  /** SNS 로그인 후 돌아온 인가 코드와 state를 서버로 넘기는 요청입니다. */
  public record CompleteRequest(
      @NotBlank @Size(max = 2000) String code, @NotBlank @Size(max = 200) String state) {}
}
