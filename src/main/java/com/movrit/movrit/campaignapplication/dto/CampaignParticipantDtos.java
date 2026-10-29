package com.movrit.movrit.campaignapplication.dto;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import java.time.LocalDateTime;
import java.util.List;

/** 업체가 자기 캠페인 신청자의 활동 정보와 완료 이력을 검토할 때 사용하는 응답 계약입니다. */
public final class CampaignParticipantDtos {
  private CampaignParticipantDtos() {}

  public record Profile(
      Long creatorId,
      String channelNickname,
      String accountNickname,
      boolean nicknameConsistent,
      String bio,
      String primaryCategory,
      String channelUrl,
      String profileStatus) {}

  public record ChannelMetric(
      String channelType,
      String metricType,
      String metricName,
      Long value,
      String measurementPeriod,
      LocalDateTime measuredAt,
      MetricVerificationStatus verificationStatus,
      String source) {}

  public record ApplicationAnswer(
      String questionId, String questionLabel, String value, String labelSource) {}

  /** 신청 당시 조건 사본과 현재 지표를 비교한 결과이며 신규 신청 가능 여부와 구분합니다. */
  public record RequirementEvaluation(
      CampaignEligibilityDtos.RequirementResponse applicationRequirementSnapshot,
      Long currentMetricValue,
      MetricVerificationStatus currentVerificationStatus,
      boolean satisfied,
      String reason,
      String evaluationBasis) {}

  public record PublicContentLink(String channelType, String contentUrl) {}

  public record CompletedCampaign(
      Long campaignId,
      String campaignTitle,
      String category,
      LocalDateTime completedAt,
      List<PublicContentLink> publicContentLinks) {}

  /** 배송 연락처·주소와 다른 캠페인의 신청 답변은 포함하지 않습니다. */
  public record DetailResponse(
      Long applicationId,
      Long campaignId,
      Profile profile,
      List<ChannelMetric> metrics,
      List<ApplicationAnswer> applicationAnswers,
      RequirementEvaluation requirementEvaluation,
      long completedCampaignCount,
      List<CompletedCampaign> completedCampaigns) {}
}
