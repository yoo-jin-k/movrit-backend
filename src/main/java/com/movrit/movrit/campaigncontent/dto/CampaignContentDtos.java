package com.movrit.movrit.campaigncontent.dto;

import com.movrit.movrit.campaigncontent.entity.ContentSubmissionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 제출 콘텐츠의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class CampaignContentDtos {
  private CampaignContentDtos() {}

  /**
   * 제출 콘텐츠의 콘텐츠 제출 및 재제출 요청을 전달합니다.
   *
   * @param contentUrl 제출한 콘텐츠 주소입니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 max = 1000입니다.
   * @param note 제출자가 남긴 설명입니다. 크기 제약은 max = 2000입니다.
   * @param expectedVersion 클라이언트가 마지막으로 조회한 버전이며 서버 현재 버전과 비교합니다.
   */
  public record SubmitRequest(
      @NotBlank @Size(max = 1000) String contentUrl,
      @Size(max = 2000) String note,
      Long expectedVersion) {}

  /**
   * 제출 콘텐츠의 콘텐츠 수정 요청 의견을 전달합니다.
   *
   * @param comment 검수 의견입니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 max = 2000입니다.
   * @param expectedVersion 클라이언트가 마지막으로 조회한 버전이며 서버 현재 버전과 비교합니다. null을 허용하지 않습니다.
   */
  public record ReviewRequest(
      @NotBlank @Size(max = 2000) String comment, @NotNull Long expectedVersion) {}

  /**
   * 제출 콘텐츠의 승인 요청을 전달합니다.
   *
   * @param comment 검수 의견입니다. 크기 제약은 max = 2000입니다.
   * @param expectedVersion 클라이언트가 마지막으로 조회한 버전이며 서버 현재 버전과 비교합니다. null을 허용하지 않습니다.
   */
  public record ApproveRequest(@Size(max = 2000) String comment, @NotNull Long expectedVersion) {}

  /**
   * 제출 콘텐츠의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param applicationId 캠페인 신청 식별자입니다.
   * @param creatorNickname 참여자의 채널 프로필 표시 이름입니다.
   * @param campaignTitle 캠페인 제목입니다.
   * @param contentUrl 제출한 콘텐츠 주소입니다.
   * @param note 제출자가 남긴 설명입니다.
   * @param status 현재 업무 상태입니다.
   * @param revisionCount 수정 요청 횟수입니다.
   * @param version 조회한 데이터 버전이며 수정 요청의 동시성 검사에 사용됩니다.
   * @param reviewComment 저장된 검수 의견입니다.
   * @param submittedAt 제출 시각입니다.
   * @param reviewedAt 검수 시각입니다.
   */
  public record Response(
      Long id,
      Long applicationId,
      String creatorNickname,
      String campaignTitle,
      String contentUrl,
      String note,
      ContentSubmissionStatus status,
      Integer revisionCount,
      Long version,
      String reviewComment,
      LocalDateTime submittedAt,
      LocalDateTime reviewedAt) {}
}
