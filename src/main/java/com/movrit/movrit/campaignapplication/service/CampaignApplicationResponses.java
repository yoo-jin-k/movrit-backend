package com.movrit.movrit.campaignapplication.service;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.dto.CampaignParticipantDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * 신청 엔티티를 화면용 응답(CampaignApplicationDtos.Response)으로 바꿉니다.
 *
 * <p>참여자·운영자 화면은 배송 연락처를 그대로 보여주고, 업체 화면은 참여자가 참여를 수락(ACTIVE)한 뒤에만 연락처를 보여줍니다. 업체 화면에는 완료 이력 수와 채널
 * 지표를 함께 담을 수 있습니다.
 */
@Component
@RequiredArgsConstructor
class CampaignApplicationResponses {
  private final CampaignApplicationJson json;

  /** 참여자 본인·운영자 화면용 응답입니다. 배송 연락처를 그대로 보여줍니다. */
  CampaignApplicationDtos.Response response(CampaignApplication application) {
    return response(application, false);
  }

  /** 업체 화면용 응답입니다. 참여 확정 전에는 배송 연락처를 숨깁니다. */
  CampaignApplicationDtos.Response sellerResponse(CampaignApplication application) {
    return sellerResponse(application, null);
  }

  CampaignApplicationDtos.Response sellerResponse(
      CampaignApplication application, Long completedCampaignCount) {
    return sellerResponse(application, completedCampaignCount, List.of());
  }

  CampaignApplicationDtos.Response sellerResponse(
      CampaignApplication application,
      Long completedCampaignCount,
      List<CampaignParticipantDtos.ChannelMetric> creatorMetrics) {
    // 선정만으로는 참여가 확정되지 않습니다. 당사자가 수락해 ACTIVE가 된 뒤에만
    // 배송에 필요한 연락처를 업체에 제공하고, 취소·미선정 건에서는 다시 숨깁니다.
    boolean hideContact =
        application.getStatus() != CampaignApplicationStatus.ACTIVE
            && application.getStatus() != CampaignApplicationStatus.CONTENT_SUBMITTED
            && application.getStatus() != CampaignApplicationStatus.REVISION_REQUESTED
            && application.getStatus() != CampaignApplicationStatus.COMPLETED;
    return response(application, hideContact, completedCampaignCount, creatorMetrics);
  }

  CampaignApplicationDtos.Response response(CampaignApplication application, boolean hideContact) {
    return response(application, hideContact, null);
  }

  CampaignApplicationDtos.Response response(
      CampaignApplication application, boolean hideContact, Long completedCampaignCount) {
    return response(application, hideContact, completedCampaignCount, List.of());
  }

  CampaignApplicationDtos.Response response(
      CampaignApplication application,
      boolean hideContact,
      Long completedCampaignCount,
      List<CampaignParticipantDtos.ChannelMetric> creatorMetrics) {
    CampaignApplicationDtos.ShippingContact contact =
        application.getFulfillmentType() == FulfillmentType.DELIVERY && !hideContact
            ? new CampaignApplicationDtos.ShippingContact(
                application.getRecipientName(),
                application.getRecipientPhone(),
                application.getPostalCode(),
                application.getAddress1(),
                application.getAddress2())
            : null;
    List<String> notices = new ArrayList<>();
    if (!application.isIdentityVerificationConfigured()) notices.add("본인 인증 기능이 아직 구성되지 않았습니다.");
    if (!application.isChannelReady()) notices.add("활동 채널 정보를 먼저 등록해 주세요.");
    return new CampaignApplicationDtos.Response(
        application.getId(),
        application.getCampaign().getId(),
        application.getCampaign().getTitle(),
        application.getCampaign().getSelectionMode(),
        // 프리오더 없이 운영하는 캠페인은 연결 프리오더 ID를 비워 응답합니다.
        application.getCampaign().getPreorder() == null
            ? null
            : application.getCampaign().getPreorder().getId(),
        application.getCreator().getId(),
        application.getCreator().getNickname(),
        application.getCreator().getSnsUrl(),
        creatorMetrics,
        application.getFulfillmentType(),
        application.getStatus(),
        application.getMessage(),
        contact,
        json.read(application.getAnswersJson(), new TypeReference<>() {}),
        json.read(application.getAgreementAcceptancesJson(), new TypeReference<>() {}),
        application.getFormVersion(),
        application.getEligibilityRequirementSnapshotJson() == null
            ? null
            : json.read(
                application.getEligibilityRequirementSnapshotJson(), new TypeReference<>() {}),
        application.getEligibilityRequirementVersion(),
        application.isIdentityVerificationConfigured(),
        application.isIdentityVerified(),
        application.isEmailVerified(),
        application.isChannelReady(),
        notices,
        application.getSubmittedAt(),
        application.getRespondedAt(),
        application.getCancelledAt(),
        application.getCancellationReason(),
        application.getExperienceConfirmedAt(),
        completedCampaignCount);
  }

  /** 지표 집계 행을 화면용 응답으로 바꾸고, 지표 종류 코드를 한국어 이름으로 바꿉니다. */
  CampaignParticipantDtos.ChannelMetric metric(
      CampaignApplicationRepository.CreatorMetricSummary metric) {
    return new CampaignParticipantDtos.ChannelMetric(
        metric.getChannelType(),
        metric.getMetricType(),
        switch (metric.getMetricType()) {
          case "FOLLOWER_COUNT" -> "팔로워 수";
          case "AVERAGE_VISITORS" -> "평균 방문자 수";
          case "SUBSCRIBER_COUNT" -> "구독자 수";
          default -> metric.getMetricType();
        },
        metric.getMetricValue(),
        metric.getMeasurementPeriod(),
        metric.getMeasuredAt(),
        com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus.valueOf(
            metric.getVerificationStatus()),
        "UNKNOWN");
  }
}
