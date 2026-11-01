package com.movrit.movrit.campaignapplication.dto;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.channelconnect.dto.ChannelConnectDtos;
import java.time.LocalDateTime;
import java.util.List;

/** 업체가 자기 캠페인 신청자의 활동 정보와 완료 이력을 검토할 때 사용하는 응답 계약입니다. */
public final class CampaignParticipantDtos {
  private CampaignParticipantDtos() {}

  /** 참여자 기본 정보(닉네임·프로필 사진·소개)입니다. */
  public record Profile(
      Long creatorId,
      String channelNickname,
      String accountNickname,
      boolean nicknameConsistent,
      String bio,
      String primaryCategory,
      String channelUrl,
      String profileStatus) {}

  /** 참여자의 채널 지표 한 건(채널·지표 종류·값·측정 기간·확인 상태)입니다. */
  public record ChannelMetric(
      String channelType,
      String metricType,
      String metricName,
      Long value,
      String measurementPeriod,
      LocalDateTime measuredAt,
      MetricVerificationStatus verificationStatus,
      String source) {}

  /** 신청서 질문과 참여자 답변 한 쌍입니다. */
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

  /** 참여자가 이전 캠페인에서 공개한 콘텐츠 링크입니다. */
  public record PublicContentLink(String channelType, String contentUrl) {}

  /** 참여자가 완료한 이전 캠페인 한 건입니다. */
  public record CompletedCampaign(
      Long campaignId,
      String campaignTitle,
      String category,
      LocalDateTime completedAt,
      List<PublicContentLink> publicContentLinks) {}

  /** 배송 연락처·주소와 다른 캠페인의 신청 답변은 포함하지 않습니다. linkedChannels는 참여자가 연결·등록한 SNS 채널(반려 제외)입니다. */
  public record DetailResponse(
      Long applicationId,
      Long campaignId,
      Profile profile,
      List<ChannelMetric> metrics,
      List<ApplicationAnswer> applicationAnswers,
      RequirementEvaluation requirementEvaluation,
      long completedCampaignCount,
      List<CompletedCampaign> completedCampaigns,
      List<ChannelConnectDtos.LinkedChannel> linkedChannels) {}
}
