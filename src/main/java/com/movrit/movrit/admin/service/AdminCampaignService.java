package com.movrit.movrit.admin.service;

import com.movrit.movrit.admin.dto.AdminCampaignDtos;
import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.repository.CampaignEligibilityRequirementRepository;
import com.movrit.movrit.campaigneligibility.service.CampaignEligibilityPolicyService;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 운영자 캠페인 관리입니다. 모든 브랜드의 캠페인을 상태·신청 현황과 함께 보고, 사용자 목록에서 숨기거나(마감) 다시 공개합니다.
 *
 * <ul>
 *   <li>숨김은 데이터를 지우지 않고 open=false로 바꿉니다. 이미 진행 중인 참여자·콘텐츠 흐름은 그대로 이어집니다.
 *   <li>숨김·다시 공개는 캠페인 업체에 알림을 보내고 감사 기록을 남깁니다.
 *   <li>운영자는 캠페인마다 신청 조건(인스타그램 팔로워 수·네이버 블로그 일평균 방문자)을 정하거나 해제할 수 있습니다. 바꾸면 업체에 알리고 감사 기록을 남깁니다. 이미
 *       신청한 참여자에게는 신청 당시 조건이 그대로 남습니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdminCampaignService {
  private static final Set<CampaignApplicationStatus> SELECTED_OR_LATER =
      EnumSet.of(
          CampaignApplicationStatus.SELECTED,
          CampaignApplicationStatus.ACTIVE,
          CampaignApplicationStatus.CONTENT_SUBMITTED,
          CampaignApplicationStatus.REVISION_REQUESTED,
          CampaignApplicationStatus.COMPLETED);

  private final CampaignRepository campaignRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final MemberService memberService;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;
  private final CampaignEligibilityPolicyService eligibilityPolicy;
  private final CampaignEligibilityRequirementRepository requirementRepository;

  /** 전체 캠페인을 최신순으로 돌려줍니다. 신청 수는 한 번의 집계 쿼리로 붙입니다. */
  @Transactional(readOnly = true)
  public List<AdminCampaignDtos.Item> list(Long adminId) {
    memberService.getAdmin(adminId);
    Map<Long, long[]> counts = new HashMap<>();
    applicationRepository
        .countAllByCampaignAndStatus()
        .forEach(
            row -> {
              long[] c = counts.computeIfAbsent(row.getCampaignId(), key -> new long[3]);
              if (row.getStatus() != CampaignApplicationStatus.CANCELLED) c[0] += row.getTotal();
              if (SELECTED_OR_LATER.contains(row.getStatus())) c[1] += row.getTotal();
              if (row.getStatus() == CampaignApplicationStatus.COMPLETED) c[2] += row.getTotal();
            });
    Map<Long, CampaignEligibilityDtos.RequirementResponse> requirements = new HashMap<>();
    requirementRepository
        .findAll()
        .forEach(
            requirement ->
                requirements.put(
                    requirement.getCampaign().getId(),
                    eligibilityPolicy.find(requirement.getCampaign().getId())));
    LocalDateTime now = LocalDateTime.now();
    return campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
        .map(
            campaign ->
                item(
                    campaign,
                    counts.getOrDefault(campaign.getId(), new long[3]),
                    now,
                    requirements.get(campaign.getId())))
        .toList();
  }

  /** 캠페인을 사용자 목록에서 숨깁니다(마감). 사유는 업체 알림과 감사 기록에 남깁니다. 이미 숨긴 캠페인이면 409입니다. */
  @Transactional
  public AdminCampaignDtos.Item close(Long adminId, Long campaignId, String reason) {
    memberService.getAdmin(adminId);
    Campaign campaign = getCampaign(campaignId);
    if (!campaign.isOpen()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 숨긴 캠페인입니다.");
    }
    campaign.hideByAdmin();
    String note = reason.trim();
    notifyBrand(campaign, "운영자가 캠페인을 마감했습니다.", "사유: " + note);
    auditLogService.record(
        adminId,
        "ADMIN_CAMPAIGN_CLOSED",
        "CAMPAIGN",
        campaignId,
        "{\"open\":true}",
        "{\"open\":false}",
        note);
    return item(campaign, new long[3], LocalDateTime.now(), eligibilityPolicy.find(campaignId));
  }

  /** 숨긴 캠페인을 다시 공개합니다. 모집 기간은 그대로이므로 기간이 지났다면 '모집 마감'으로 보입니다. */
  @Transactional
  public AdminCampaignDtos.Item reopen(Long adminId, Long campaignId) {
    memberService.getAdmin(adminId);
    Campaign campaign = getCampaign(campaignId);
    if (campaign.isOpen()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 공개 중인 캠페인입니다.");
    }
    campaign.reopen();
    notifyBrand(campaign, "운영자가 캠페인을 다시 공개했습니다.", campaign.getTitle());
    auditLogService.record(
        adminId,
        "ADMIN_CAMPAIGN_REOPENED",
        "CAMPAIGN",
        campaignId,
        "{\"open\":false}",
        "{\"open\":true}",
        null);
    return item(campaign, new long[3], LocalDateTime.now(), eligibilityPolicy.find(campaignId));
  }

  /** 운영자가 캠페인의 현재 신청 조건을 조회합니다. 조건이 없으면 null입니다. */
  @Transactional(readOnly = true)
  public CampaignEligibilityDtos.RequirementResponse requirement(Long adminId, Long campaignId) {
    memberService.getAdmin(adminId);
    getCampaign(campaignId);
    return eligibilityPolicy.find(campaignId);
  }

  /**
   * 운영자가 캠페인 신청 조건을 정하거나 고칩니다. 버전이 다르면 409로 거절해 업체가 먼저 바꾼 조건을 덮어쓰지 않습니다. 업체에 알리고 바뀌기 전·후 조건을 감사 기록에
   * 남깁니다.
   */
  @Transactional
  public CampaignEligibilityDtos.RequirementResponse saveRequirement(
      Long adminId, Long campaignId, CampaignEligibilityDtos.RequirementSaveRequest request) {
    memberService.getAdmin(adminId);
    Campaign campaign = getCampaign(campaignId);
    CampaignEligibilityDtos.RequirementResponse before = eligibilityPolicy.find(campaignId);
    CampaignEligibilityDtos.RequirementResponse saved =
        eligibilityPolicy.saveAsOperator(campaignId, request);
    notifyBrand(campaign, "운영자가 캠페인 신청 조건을 정했습니다.", describe(saved));
    auditLogService.record(
        adminId,
        "ADMIN_CAMPAIGN_REQUIREMENT_SAVED",
        "CAMPAIGN",
        campaignId,
        json(before),
        json(saved),
        null);
    return saved;
  }

  /** 운영자가 캠페인 신청 조건을 해제합니다. 업체에 알리고 감사 기록을 남깁니다. */
  @Transactional
  public void deleteRequirement(Long adminId, Long campaignId, Long expectedVersion) {
    memberService.getAdmin(adminId);
    Campaign campaign = getCampaign(campaignId);
    CampaignEligibilityDtos.RequirementResponse before = eligibilityPolicy.find(campaignId);
    eligibilityPolicy.deleteAsOperator(campaignId, expectedVersion);
    notifyBrand(campaign, "운영자가 캠페인 신청 조건을 해제했습니다.", campaign.getTitle());
    auditLogService.record(
        adminId,
        "ADMIN_CAMPAIGN_REQUIREMENT_DELETED",
        "CAMPAIGN",
        campaignId,
        json(before),
        null,
        null);
  }

  /** 알림에 쓸 조건 설명입니다. */
  static String describe(CampaignEligibilityDtos.RequirementResponse requirement) {
    String minimum = String.format("%,d", requirement.minimumValue());
    if (CampaignEligibilityPolicyService.DAILY_VISITORS.equals(requirement.metricType())) {
      return "네이버 블로그 일평균 방문자 " + minimum + "명 이상(최근 4일 평균)";
    }
    return "인스타그램 팔로워 " + minimum + "명 이상";
  }

  /** 감사 기록용으로 조건을 짧은 JSON으로 적습니다. 조건이 없으면 null입니다. */
  private static String json(CampaignEligibilityDtos.RequirementResponse requirement) {
    if (requirement == null) return null;
    return "{\"channelType\":\""
        + requirement.channelType()
        + "\",\"metricType\":\""
        + requirement.metricType()
        + "\",\"minimumValue\":"
        + requirement.minimumValue()
        + "}";
  }

  private void notifyBrand(Campaign campaign, String title, String message) {
    if (campaign.getBrand() == null || campaign.getBrand().getSeller() == null) return;
    notificationService.send(
        campaign.getBrand().getSeller().getMember().getId(),
        "CAMPAIGN",
        title,
        message,
        "CAMPAIGN",
        campaign.getId());
  }

  private Campaign getCampaign(Long campaignId) {
    return campaignRepository
        .findById(campaignId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
  }

  /** 공개 여부와 모집 기간으로 표시 상태를 정합니다. */
  static String state(Campaign campaign, LocalDateTime now) {
    if (!campaign.isOpen()) return "HIDDEN";
    if (now.isBefore(campaign.getStartAt())) return "UPCOMING";
    return now.isBefore(campaign.getEndAt()) ? "RECRUITING" : "ENDED";
  }

  private static AdminCampaignDtos.Item item(
      Campaign campaign,
      long[] counts,
      LocalDateTime now,
      CampaignEligibilityDtos.RequirementResponse requirement) {
    return new AdminCampaignDtos.Item(
        campaign.getId(),
        campaign.getTitle(),
        campaign.getBrand() == null ? null : campaign.getBrand().getName(),
        campaign.getCampaignType() == null ? null : campaign.getCampaignType().name(),
        campaign.getSelectionMode() == null ? null : campaign.getSelectionMode().name(),
        state(campaign, now),
        campaign.isOpen(),
        campaign.getStartAt(),
        campaign.getEndAt(),
        campaign.getRecruitmentCount(),
        counts[0],
        counts[1],
        counts[2],
        campaign.getCreatedAt(),
        requirement);
  }
}
