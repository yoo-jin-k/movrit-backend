package com.movrit.movrit.campaigneligibility.dto;

import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** 캠페인 참여 조건의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class CampaignEligibilityDtos {
  private CampaignEligibilityDtos() {}

  /**
   * 캠페인 참여 조건의 채널 지표 저장 요청을 전달합니다.
   *
   * @param channelType 지표를 측정한 채널 코드입니다. 빈 문자열을 허용하지 않습니다.
   * @param metricType 팔로워 수 또는 방문자 수 등 지표 코드입니다. 빈 문자열을 허용하지 않습니다.
   * @param value 측정한 지표값이며 음수는 허용하지 않습니다.
   * @param measurementPeriod 지표 집계 기간 식별값이며, 기간 정책을 이 DTO에서 확정하지 않습니다. 크기 제약은 max = 40입니다.
   * @param measuredAt 지표 측정 시각입니다.
   */
  public record MetricSaveRequest(
      @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,39}$") String channelType,
      @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,39}$") String metricType,
      @NotNull @Min(0) Long value,
      @Size(max = 40) String measurementPeriod,
      LocalDateTime measuredAt) {}

  /**
   * 캠페인 참여 조건의 채널 지표 응답을 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param channelType 지표를 측정한 채널 코드입니다.
   * @param metricType 팔로워 수 또는 방문자 수 등 지표 코드입니다.
   * @param value 측정한 지표값이며 음수는 허용하지 않습니다.
   * @param measurementPeriod 지표 집계 기간 식별값이며, 기간 정책을 이 DTO에서 확정하지 않습니다.
   * @param measuredAt 지표 측정 시각입니다.
   * @param verificationStatus 지표의 검증 상태이며, 입력값을 실제 외부 검증 완료로 간주하지 않습니다.
   */
  public record MetricResponse(
      Long id,
      String channelType,
      String metricType,
      Long value,
      String measurementPeriod,
      LocalDateTime measuredAt,
      MetricVerificationStatus verificationStatus) {}

  /**
   * 캠페인 참여 조건의 채널 프로필 응답을 전달합니다.
   *
   * @param profileExists 채널 프로필 존재 여부입니다.
   * @param metrics 채널별 지표 목록입니다.
   */
  public record ChannelProfileResponse(boolean profileExists, List<MetricResponse> metrics) {}

  /**
   * 캠페인 참여 조건의 캠페인 참여 조건 저장 요청을 전달합니다.
   *
   * @param channelType 지표를 측정한 채널 코드입니다. 빈 문자열을 허용하지 않습니다.
   * @param metricType 팔로워 수 또는 방문자 수 등 지표 코드입니다. 빈 문자열을 허용하지 않습니다.
   * @param minimumValue 해당 캠페인에서 업체가 설정한 최소 지표값이며 전역 기준이 아닙니다. null을 허용하지 않습니다. 최솟값은 0입니다.
   * @param measurementPeriod 지표 집계 기간 식별값이며, 기간 정책을 이 DTO에서 확정하지 않습니다. 크기 제약은 max = 40입니다.
   * @param expectedVersion 클라이언트가 마지막으로 조회한 버전이며 서버 현재 버전과 비교합니다. 최솟값은 0입니다.
   */
  public record RequirementSaveRequest(
      @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,39}$") String channelType,
      @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,39}$") String metricType,
      @NotNull @Min(0) Long minimumValue,
      @Size(max = 40) String measurementPeriod,
      @Min(0) Long expectedVersion) {}

  /**
   * 캠페인 참여 조건의 캠페인 참여 조건 응답을 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param campaignId 캠페인 식별자입니다.
   * @param channelType 지표를 측정한 채널 코드입니다.
   * @param metricType 팔로워 수 또는 방문자 수 등 지표 코드입니다.
   * @param minimumValue 해당 캠페인에서 업체가 설정한 최소 지표값이며 전역 기준이 아닙니다.
   * @param measurementPeriod 지표 집계 기간 식별값이며, 기간 정책을 이 DTO에서 확정하지 않습니다.
   * @param version 조회한 데이터 버전이며 수정 요청의 동시성 검사에 사용됩니다.
   */
  public record RequirementResponse(
      Long id,
      Long campaignId,
      String channelType,
      String metricType,
      Long minimumValue,
      String measurementPeriod,
      Long version) {}

  /**
   * 캠페인 참여 조건의 채널 지표 조건 판정 결과를 전달합니다.
   *
   * @param channelType 지표를 측정한 채널 코드입니다.
   * @param metricType 팔로워 수 또는 방문자 수 등 지표 코드입니다.
   * @param requiredMinimum 참여 판정에 필요한 최소 지표값입니다.
   * @param actualValue 판정에 사용한 사용자 지표값입니다.
   * @param verificationStatus 지표의 검증 상태이며, 입력값을 실제 외부 검증 완료로 간주하지 않습니다.
   * @param satisfied 해당 지표 조건의 충족 여부입니다.
   * @param reason 처리 또는 판정 사유입니다.
   */
  public record MetricEvaluation(
      String channelType,
      String metricType,
      Long requiredMinimum,
      Long actualValue,
      MetricVerificationStatus verificationStatus,
      boolean satisfied,
      String reason) {}
}
