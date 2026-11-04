package com.movrit.movrit.channelconnect.dto;

import com.movrit.movrit.channelconnect.entity.ChannelConnectionMethod;
import com.movrit.movrit.channelconnect.entity.ChannelVerificationStatus;
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

  /**
   * 연결된 계정 정보입니다. 접근 토큰과 캡처 저장 위치는 담지 않습니다.
   *
   * @param method 연결 방법(OAUTH 로그인 연결 · MANUAL 직접 등록)
   * @param verificationStatus 확인 상태(VERIFIED · PENDING · REJECTED)
   * @param reviewNote 반려 사유. 반려가 아니면 null
   */
  public record Connection(
      String accountName,
      String profileUrl,
      Long followerCount,
      LocalDateTime connectedAt,
      ChannelConnectionMethod method,
      ChannelVerificationStatus verificationStatus,
      String reviewNote) {
    public static Connection from(MemberChannelConnection value) {
      return new Connection(
          value.getAccountName(),
          value.getProfileUrl(),
          value.getFollowerCount(),
          value.getConnectedAt(),
          value.getConnectionMethod(),
          value.getVerificationStatus(),
          value.getReviewNote());
    }
  }

  /**
   * 업체가 참여자 상세에서 보는 연결 채널입니다. 반려된 채널은 보여주지 않습니다.
   *
   * @param provider 서비스 주소 이름(instagram·youtube·naver-blog)
   */
  public record LinkedChannel(
      String provider,
      String label,
      String accountName,
      String profileUrl,
      Long followerCount,
      ChannelConnectionMethod method,
      ChannelVerificationStatus verificationStatus) {
    public static LinkedChannel from(MemberChannelConnection value) {
      return new LinkedChannel(
          value.getProvider().slug(),
          value.getProvider().label(),
          value.getAccountName(),
          value.getProfileUrl(),
          value.getFollowerCount(),
          value.getConnectionMethod(),
          value.getVerificationStatus());
    }
  }

  /** 운영자 '채널 확인' 목록의 한 줄입니다. 캡처는 별도 주소(/evidence)로 받습니다. */
  public record ReviewItem(
      Long id,
      Long memberId,
      String memberNickname,
      String memberEmail,
      String provider,
      String label,
      String accountName,
      String profileUrl,
      Long followerCount,
      ChannelVerificationStatus verificationStatus,
      String reviewNote,
      LocalDateTime requestedAt,
      LocalDateTime reviewedAt,
      boolean hasEvidence,
      ChannelConnectionMethod method,
      String verificationCode) {
    public static ReviewItem from(MemberChannelConnection value) {
      return new ReviewItem(
          value.getId(),
          value.getMember().getId(),
          value.getMember().getNickname(),
          value.getMember().getEmail(),
          value.getProvider().slug(),
          value.getProvider().label(),
          value.getAccountName(),
          value.getProfileUrl(),
          value.getFollowerCount(),
          value.getVerificationStatus(),
          value.getReviewNote(),
          value.getConnectedAt(),
          value.getReviewedAt(),
          value.getEvidenceKey() != null,
          value.getConnectionMethod(),
          value.getVerificationCode());
    }
  }

  /** 운영자 반려 요청입니다. 사유는 회원에게 그대로 보여줍니다. */
  public record RejectRequest(@NotBlank @Size(max = 500) String reason) {}

  /**
   * 소개글 인증 코드입니다.
   *
   * @param checksLeft 이 코드로 남은 확인 횟수
   * @param autoCheck 이 서비스를 지금 자동으로 확인할 수 있는지(인스타그램은 서버 키가 없으면 운영자 확인으로 넘어감)
   */
  public record BioCode(String code, LocalDateTime expiresAt, int checksLeft, boolean autoCheck) {}

  /** 소개글 코드 확인 요청입니다. 팔로워 수는 운영자 확인으로 넘어갈 때(인스타그램 자동 확인 불가) 필요합니다. */
  public record BioVerifyRequest(
      @NotBlank @Size(max = 500) String profileUrl, Long followerCount) {}

  /**
   * 소개글 코드 확인 결과입니다.
   *
   * @param result VERIFIED(바로 확인됨) · PENDING_REVIEW(운영자 확인 대기)
   */
  public record BioVerifyResult(String result, String message, ProviderStatus status) {}

  /** 연결 시작 응답입니다. 화면은 이 주소로 이동해 SNS 로그인·동의를 진행합니다. */
  public record StartResponse(String authorizeUrl) {}

  /** SNS 로그인 후 돌아온 인가 코드와 state를 서버로 넘기는 요청입니다. */
  public record CompleteRequest(
      @NotBlank @Size(max = 2000) String code, @NotBlank @Size(max = 200) String state) {}
}
