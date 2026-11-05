package com.movrit.movrit.campaigneligibility.service;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.entity.CampaignEligibilityRequirement;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.CampaignEligibilityRequirementRepository;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 캠페인 신청 조건을 저장·해제하고, 신청하려는 회원이 조건을 충족하는지 판정합니다. 조건 설정은 캠페인 소유 업체(또는 운영자)만 할 수 있고, 판정은 서버가 회원의 확인된
 * 지표로만 합니다.
 *
 * <p>지원하는 조건은 두 가지입니다.
 *
 * <ul>
 *   <li>인스타그램 팔로워 수(INSTAGRAM · FOLLOWER_COUNT): 기간 없는 지표입니다.
 *   <li>네이버 블로그 일평균 방문자(NAVER_BLOG · DAILY_VISITORS): 블로그 방문자 위젯의 최근 4일 평균입니다. 평소에는 공개하지 않아도 되고, 이
 *       조건이 있는 캠페인에 신청할 때만 확인합니다. 확인한 지 하루가 지나면 다시 확인해야 합니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignEligibilityPolicyService {
  public static final String INSTAGRAM = "INSTAGRAM";
  public static final String FOLLOWER_COUNT = "FOLLOWER_COUNT";
  public static final String NAVER_BLOG = "NAVER_BLOG";
  public static final String DAILY_VISITORS = "DAILY_VISITORS";

  /** 방문자 위젯이 주는 최근 5일 중 오늘을 뺀 4일의 평균이라는 뜻입니다. */
  public static final String RECENT_4_DAYS = "RECENT_4_DAYS";

  /** 방문자 수는 신청 직전에 확인한 값만 씁니다. 확인한 지 이 시간이 지나면 다시 확인해야 합니다. */
  static final long VISITOR_FRESH_HOURS = 24;

  private final CampaignEligibilityRequirementRepository requirementRepository;
  private final MemberChannelMetricRepository metricRepository;
  private final CampaignRepository campaignRepository;
  private final CampaignOwnershipService campaignOwnership;

  /**
   * 조건 판정 결과입니다. blockingReason이 null이면 신청할 수 있고, 값이 있으면 신청을 막는 이유 코드입니다. measuredAt은 판정에 쓴 지표를 확인한
   * 시각으로, 신청할 때 신청 기록에 함께 남깁니다.
   */
  public record Evaluation(
      CampaignEligibilityDtos.RequirementResponse requirement,
      CampaignEligibilityDtos.MetricEvaluation metricEvaluation,
      String blockingReason,
      LocalDateTime measuredAt) {
    public Evaluation(
        CampaignEligibilityDtos.RequirementResponse requirement,
        CampaignEligibilityDtos.MetricEvaluation metricEvaluation,
        String blockingReason) {
      this(requirement, metricEvaluation, blockingReason, null);
    }
  }

  /**
   * 회원이 캠페인 신청 조건을 충족하는지 판정합니다.
   *
   * <p>조건이 없으면 바로 통과입니다. 조건이 있으면 아래 순서로 확인합니다. 지표 미입력(CHANNEL_METRIC_REQUIRED) → 미확인
   * 지표(CHANNEL_METRIC_UNVERIFIED) → 방문자 수를 확인한 지 하루 지남(CHANNEL_METRIC_STALE) → 최소값
   * 미달(CHANNEL_METRIC_BELOW_MINIMUM).
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
      return result(value, null, "CHANNEL_METRIC_REQUIRED");
    }
    MemberChannelMetric actual = metric.get();
    if (actual.getVerificationStatus() != MetricVerificationStatus.VERIFIED) {
      return result(value, actual, "CHANNEL_METRIC_UNVERIFIED");
    }
    if (DAILY_VISITORS.equals(value.getMetricType()) && !fresh(actual.getMeasuredAt())) {
      return result(value, actual, "CHANNEL_METRIC_STALE");
    }
    if (actual.getValue() < value.getMinimumValue()) {
      return result(value, actual, "CHANNEL_METRIC_BELOW_MINIMUM");
    }
    return result(value, actual, null);
  }

  /**
   * 업체가 캠페인 신청 조건을 새로 만들거나 고칩니다.
   *
   * <p>처음 만들 때는 expectedVersion을 비워 보내야 하고, 고칠 때는 현재 버전과 같아야 합니다. 다른 화면에서 먼저 바뀌었으면 409로 거절해 최신 조건을
   * 덮어쓰지 않습니다.
   */
  @Transactional
  public CampaignEligibilityDtos.RequirementResponse save(
      Long sellerMemberId,
      Long campaignId,
      CampaignEligibilityDtos.RequirementSaveRequest request) {
    return saveFor(ownedCampaign(sellerMemberId, campaignId), request);
  }

  /** 운영자가 어느 캠페인이든 신청 조건을 만들거나 고칩니다. 운영자 권한 확인은 호출한 운영자 서비스가 먼저 합니다. 버전 규칙은 업체와 같습니다. */
  @Transactional
  public CampaignEligibilityDtos.RequirementResponse saveAsOperator(
      Long campaignId, CampaignEligibilityDtos.RequirementSaveRequest request) {
    return saveFor(campaign(campaignId), request);
  }

  /** 업체가 캠페인 신청 조건을 해제합니다. 현재 버전과 다르면 409로 거절합니다. */
  @Transactional
  public void delete(Long sellerMemberId, Long campaignId, Long expectedVersion) {
    ownedCampaign(sellerMemberId, campaignId);
    deleteFor(campaignId, expectedVersion);
  }

  /** 운영자가 캠페인 신청 조건을 해제합니다. 운영자 권한 확인은 호출한 운영자 서비스가 먼저 합니다. */
  @Transactional
  public void deleteAsOperator(Long campaignId, Long expectedVersion) {
    campaign(campaignId);
    deleteFor(campaignId, expectedVersion);
  }

  /** 캠페인의 현재 신청 조건을 조회합니다. 조건이 없으면 null입니다. */
  public CampaignEligibilityDtos.RequirementResponse find(Long campaignId) {
    return requirementRepository.findByCampaignId(campaignId).map(this::response).orElse(null);
  }

  /** 확인 시각이 하루 안인지 봅니다. */
  static boolean fresh(LocalDateTime measuredAt) {
    return measuredAt != null
        && !measuredAt.isBefore(LocalDateTime.now().minusHours(VISITOR_FRESH_HOURS));
  }

  private CampaignEligibilityDtos.RequirementResponse saveFor(
      Campaign campaign, CampaignEligibilityDtos.RequirementSaveRequest request) {
    String channelType = request.channelType().trim().toUpperCase();
    String metricType = request.metricType().trim().toUpperCase();
    String period = supportedPeriod(channelType, metricType, request.measurementPeriod());
    CampaignEligibilityRequirement requirement =
        requirementRepository
            .findByCampaignId(campaign.getId())
            .orElseGet(() -> CampaignEligibilityRequirement.builder().campaign(campaign).build());
    if (requirement.getId() == null && request.expectedVersion() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
    }
    if (requirement.getId() != null
        && !requirement.getVersion().equals(request.expectedVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
    }
    requirement.update(channelType, metricType, request.minimumValue(), period);
    // @Version 값은 flush 시점에 증가하므로 응답을 만들기 전에 DB 반영을 끝냅니다.
    return response(requirementRepository.saveAndFlush(requirement));
  }

  private void deleteFor(Long campaignId, Long expectedVersion) {
    CampaignEligibilityRequirement requirement =
        requirementRepository
            .findByCampaignId(campaignId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인 조건이 없습니다."));
    if (!requirement.getVersion().equals(expectedVersion)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
    }
    requirementRepository.delete(requirement);
  }

  private Evaluation result(
      CampaignEligibilityRequirement requirement, MemberChannelMetric actual, String reason) {
    return new Evaluation(
        response(requirement),
        new CampaignEligibilityDtos.MetricEvaluation(
            requirement.getChannelType(),
            requirement.getMetricType(),
            requirement.getMinimumValue(),
            actual == null ? null : actual.getValue(),
            actual == null ? null : actual.getVerificationStatus(),
            reason == null,
            reason),
        reason,
        actual == null ? null : actual.getMeasuredAt());
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

  /**
   * 지원하는 조건인지 확인하고 저장할 집계 기간을 돌려줍니다. 팔로워 수는 기간이 없는 지표라 기간 입력을 막고, 방문자 수는 위젯 기준인 최근 4일
   * 평균(RECENT_4_DAYS)으로 맞춥니다.
   */
  static String supportedPeriod(String channelType, String metricType, String measurementPeriod) {
    boolean blank = measurementPeriod == null || measurementPeriod.isBlank();
    if (INSTAGRAM.equals(channelType) && FOLLOWER_COUNT.equals(metricType)) {
      if (!blank) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "팔로워 수 조건에는 집계 기간을 입력하지 않습니다.");
      }
      return null;
    }
    if (NAVER_BLOG.equals(channelType) && DAILY_VISITORS.equals(metricType)) {
      if (!blank && !RECENT_4_DAYS.equals(measurementPeriod.trim())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "블로그 방문자 조건의 집계 기간은 최근 4일 평균(RECENT_4_DAYS)입니다.");
      }
      return RECENT_4_DAYS;
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "지원하는 캠페인 조건은 인스타그램 팔로워 수와 네이버 블로그 일평균 방문자입니다.");
  }

  private Campaign campaign(Long campaignId) {
    return campaignRepository
        .findById(campaignId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
  }

  /** 캠페인을 찾고 요청한 회원이 소유 업체인지 확인합니다. */
  private Campaign ownedCampaign(Long memberId, Long campaignId) {
    Campaign campaign = campaign(campaignId);
    campaignOwnership.requireOwner(memberId, campaign);
    return campaign;
  }
}
