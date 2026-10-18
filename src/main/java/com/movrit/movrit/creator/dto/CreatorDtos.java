package com.movrit.movrit.creator.dto;

import com.movrit.movrit.creator.entity.Creator;
import jakarta.validation.constraints.*;

/** 채널 프로필의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class CreatorDtos {
  private CreatorDtos() {}

  /**
   * 채널 프로필의 저장 요청을 전달합니다.
   *
   * @param nickname 채널 프로필 표시 이름입니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 max = 100입니다.
   * @param bio 프로필 소개입니다. 크기 제약은 max = 2000입니다.
   * @param primaryCategory 주요 활동 분야입니다. 크기 제약은 max = 100입니다.
   * @param snsUrl 활동 채널 주소입니다. 크기 제약은 max = 500입니다.
   * @param followerCount 기존 채널 프로필의 팔로워 수입니다. null을 허용하지 않습니다. 최솟값은 0입니다.
   */
  public record SaveRequest(
      @NotBlank @Size(max = 100) String nickname,
      @Size(max = 2000) String bio,
      @Size(max = 100) String primaryCategory,
      @Size(max = 500) String snsUrl,
      @NotNull @Min(0) Long followerCount) {}

  /**
   * 채널 프로필의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param memberId 회원 식별자입니다.
   * @param nickname 채널 프로필 표시 이름입니다.
   * @param bio 프로필 소개입니다.
   * @param primaryCategory 주요 활동 분야입니다.
   * @param snsUrl 활동 채널 주소입니다.
   * @param followerCount 기존 채널 프로필의 팔로워 수입니다.
   * @param status 현재 업무 상태입니다.
   */
  public record Response(
      Long id,
      Long memberId,
      String nickname,
      String bio,
      String primaryCategory,
      String snsUrl,
      Long followerCount,
      String status) {
    /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
    public static Response from(Creator c) {
      return new Response(
          c.getId(),
          c.getMember().getId(),
          c.getNickname(),
          c.getBio(),
          c.getPrimaryCategory(),
          c.getSnsUrl(),
          c.getFollowerCount(),
          c.getStatus());
    }
  }
}
