package com.movrit.movrit.campaigneligibility.service;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.entity.CampaignEligibilityRequirement;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.CampaignEligibilityRequirementRepository;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.product.service.ProductService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 캠페인 신청 조건을 저장·해제하고, 신청하려는 회원이 조건을 충족하는지 판정합니다. 조건 설정은 캠페인 소유 업체만 할 수 있고, 판정은 서버가 회원의 확인된 지표로만
 * 합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignEligibilityPolicyService {
  private final CampaignEligibilityRequirementRepository requirementRepository;
  private final MemberChannelMetricRepository metricRepository;
  private final CampaignRepository campaignRepository;
  private final ProductService productService;

  /** 조건 판정 결과입니다. blockingReason이 null이면 신청할 수 있고, 값이 있으면 신청을 막는 이유 코드입니다. */
  public record Evaluation(
      CampaignEligibilityDtos.RequirementResponse requirement,
      CampaignEligibilityDtos.MetricEvaluation metricEvaluation,
      String blockingReason) {}

  /**
   * 회원이 캠페인 신청 조건을 충족하는지 판정합니다.
   *
   * <p>조건이 없으면 바로 통과입니다. 조건이 있으면 아래 순서로 확인합니다. 지표 미입력(CHANNEL_METRIC_REQUIRED) → 미확인
   * 지표(CHANNEL_METRIC_UNVERIFIED) → 최소값 미달(CHANNEL_METRIC_BELOW_MINIMUM).
   */
  public Evaluation evaluate(Long memberId, Long campaignId) {
    Optional<CampaignEligibilityRequirement> requirement =
        requirementRepository.findByCampaignId(campaignId);
    if (requirement.isEmpty()) {
      return new Evaluation(null, null, null);
    }
    CampaignEligibilityRequirement value = requirement.get();
    Optional<MemberChannelMetric> metric =
        metricRepository.findByMemberIdAndChannelTypeAndMetricType(
            memberId, value.getChannelType(), value.getMetricType());
    if (metric.isEmpty()) {
      return result(value, null, null, "CHANNEL_METRIC_REQUIRED");
    }
    MemberChannelMetric actual = metric.get();
    if (actual.getVerificationStatus() != MetricVerificationStatus.VERIFIED) {
      return result(
          value, actual.getValue(), actual.getVerificationStatus(), "CHANNEL_METRIC_UNVERIFIED");
    }
    if (actual.getValue() < value.getMinimumValue()) {
      return result(
          value, actual.getValue(), actual.getVerificationStatus(), "CHANNEL_METRIC_BELOW_MINIMUM");
    }
    return result(value, actual.getValue(), actual.getVerificationStatus(), null);
  }

  /**
   * 업체가 캠페인 신청 조건을 새로 만들거나 고칩니다. 현재는 인스타그램 팔로워 수 조건만 지원합니다.
   *
   * <p>처음 만들 때는 expectedVersion을 비워 보내야 하고, 고칠 때는 현재 버전과 같아야 합니다. 다른 화면에서 먼저 바뀌었으면 409로 거절해 최신 조건을
   * 덮어쓰지 않습니다.
   */
  @Transactional
  public CampaignEligibilityDtos.RequirementResponse save(
      Long sellerMemberId,
      Long campaignId,
      CampaignEligibilityDtos.RequirementSaveRequest request) {
    Campaign campaign = ownedCampaign(sellerMemberId, campaignId);
    String channelType = request.channelType().trim().toUpperCase();
    String metricType = request.metricType().trim().toUpperCase();
    validateSupportedMetric(channelType, metricType, request.measurementPeriod());
    CampaignEligibilityRequirement requirement =
        requirementRepository
            .findByCampaignId(campaignId)
            .orElseGet(() -> CampaignEligibilityRequirement.builder().campaign(campaign).build());
    if (requirement.getId() == null && request.expectedVersion() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
    }
    if (requirement.getId() != null
        && !requirement.getVersion().equals(request.expectedVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
    }
    requirement.update(
        channelType, metricType, request.minimumValue(), normalize(request.measurementPeriod()));
    // @Version 값은 flush 시점에 증가하므로 응답을 만들기 전에 DB 반영을 끝냅니다.
    return response(requirementRepository.saveAndFlush(requirement));
  }

  /** 업체가 캠페인 신청 조건을 해제합니다. 현재 버전과 다르면 409로 거절합니다. */
  @Transactional
  public void delete(Long sellerMemberId, Long campaignId, Long expectedVersion) {
    ownedCampaign(sellerMemberId, campaignId);
    CampaignEligibilityRequirement requirement =
        requirementRepository
            .findByCampaignId(campaignId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인 조건이 없습니다."));
    if (!requirement.getVersion().equals(expectedVersion)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
    }
    requirementRepository.delete(requirement);
  }

  /** 캠페인의 현재 신청 조건을 조회합니다. 조건이 없으면 null입니다. */
  public CampaignEligibilityDtos.RequirementResponse find(Long campaignId) {
    return requirementRepository.findByCampaignId(campaignId).map(this::response).orElse(null);
  }

  private Evaluation result(
      CampaignEligibilityRequirement requirement,
      Long actual,
      MetricVerificationStatus status,
      String reason) {
    return new Evaluation(
        response(requirement),
        new CampaignEligibilityDtos.MetricEvaluation(
            requirement.getChannelType(),
            requirement.getMetricType(),
            requirement.getMinimumValue(),
            actual,
            status,
            reason == null,
            reason),
        reason);
  }

  private CampaignEligibilityDtos.RequirementResponse response(
      CampaignEligibilityRequirement requirement) {
    return new CampaignEligibilityDtos.RequirementResponse(
        requirement.getId(),
        requirement.getCampaign().getId(),
        requirement.getChannelType(),
        requirement.getMetricType(),
        requirement.getMinimumValue(),
        requirement.getMeasurementPeriod(),
        requirement.getVersion());
  }

  /** 지원하는 조건(인스타그램 팔로워 수)인지 확인합니다. 팔로워 수는 기간이 없는 지표라 집계 기간 입력을 막습니다. */
  private void validateSupportedMetric(
      String channelType, String metricType, String measurementPeriod) {
    if (!"INSTAGRAM".equals(channelType) || !"FOLLOWER_COUNT".equals(metricType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 지원하는 캠페인 조건은 인스타그램 팔로워 수입니다.");
    }
    if (measurementPeriod != null && !measurementPeriod.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "팔로워 수 조건에는 집계 기간을 입력하지 않습니다.");
    }
  }

  /** 캠페인을 찾고 요청한 회원이 소유 업체인지 확인합니다. */
  private Campaign ownedCampaign(Long memberId, Long campaignId) {
    Campaign campaign =
        campaignRepository
            .findById(campaignId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    productService.getOwnedProduct(memberId, campaign.getPreorder().getProduct().getId());
    return campaign;
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
