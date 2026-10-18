package com.movrit.movrit.campaigneligibility.dto;

import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class CampaignEligibilityDtos {
  private CampaignEligibilityDtos() {}

  public record MetricSaveRequest(
      @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,39}$") String channelType,
      @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,39}$") String metricType,
      @NotNull @Min(0) Long value,
      @Size(max = 40) String measurementPeriod,
      LocalDateTime measuredAt) {}

  public record MetricResponse(
      Long id,
      String channelType,
      String metricType,
      Long value,
      String measurementPeriod,
      LocalDateTime measuredAt,
      MetricVerificationStatus verificationStatus) {}

  public record ChannelProfileResponse(boolean profileExists, List<MetricResponse> metrics) {}

  public record RequirementSaveRequest(
      @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,39}$") String channelType,
      @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,39}$") String metricType,
      @NotNull @Min(0) Long minimumValue,
      @Size(max = 40) String measurementPeriod,
      @Min(0) Long expectedVersion) {}

  public record RequirementResponse(
      Long id,
      Long campaignId,
      String channelType,
      String metricType,
      Long minimumValue,
      String measurementPeriod,
      Long version) {}

  public record MetricEvaluation(
      String channelType,
      String metricType,
      Long requiredMinimum,
      Long actualValue,
      MetricVerificationStatus verificationStatus,
      boolean satisfied,
      String reason) {}
}
