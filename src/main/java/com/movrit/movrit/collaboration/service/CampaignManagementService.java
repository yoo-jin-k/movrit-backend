package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignmedia.service.CampaignPublicPolicy;
import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignSelectionMode;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignManagementService {
  private final CampaignRepository campaignRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final CampaignApplicationFormRepository formRepository;
  private final ProductService productService;
  private final AuditLogService auditLogService;

  public CampaignManagementDtos.DetailResponse getDetail(Long campaignId) {
    Campaign campaign = getCampaign(campaignId);
    CampaignPublicPolicy.requireVisible(campaign);
    boolean visit =
        formRepository
            .findByCampaignId(campaignId)
            .map(CampaignPublicPolicy::includesVisit)
            .orElse(false);
    return CampaignManagementDtos.DetailResponse.from(campaign, visit);
  }

  @Transactional
  public CampaignManagementDtos.DetailResponse update(
      Long memberId, Long campaignId, CampaignManagementDtos.UpdateRequest request) {
    Campaign campaign = getOwnedCampaign(memberId, campaignId);
    if (!java.util.Objects.equals(
        campaign.getScheduleVersion(), request.expectedScheduleVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 일정이 변경되었습니다. 최신 일정을 다시 확인해 주세요.");
    }
    CampaignSelectionMode requestedSelectionMode =
        request.selectionMode() == null ? campaign.getSelectionMode() : request.selectionMode();
    if (requestedSelectionMode != campaign.getSelectionMode()
        && applicationRepository.existsByCampaignId(campaignId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "신청 이력이 있는 캠페인은 선정 방식을 변경할 수 없습니다.");
    }
    try {
      campaign.update(
          request.title().trim(),
          normalize(request.description()),
          normalize(request.category()),
          request.minFollowerCount(),
          request.recruitmentCount(),
          requestedSelectionMode,
          normalize(request.rewardDescription()),
          normalize(request.mission()),
          normalize(request.channelGuide()),
          normalize(request.scheduleGuide()),
          normalize(request.deliveryGuide()),
          normalize(request.visitGuide()),
          normalize(request.caution()),
          normalize(request.submissionRequirement()),
          request.expectedScheduleVersion(),
          request.startAt(),
          request.endAt(),
          request.selectionAnnouncedAt(),
          request.experienceStartAt(),
          request.experienceEndAt(),
          request.contentSubmissionStartAt(),
          request.contentSubmissionEndAt(),
          request.resultsAnnouncedAt());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
    auditLogService.record(memberId, "CAMPAIGN_UPDATED", "CAMPAIGN", campaignId, null, null, null);
    return CampaignManagementDtos.DetailResponse.from(campaign);
  }

  @Transactional
  public CampaignManagementDtos.DetailResponse close(Long memberId, Long campaignId) {
    Campaign campaign = getOwnedCampaign(memberId, campaignId);
    campaign.close();
    auditLogService.record(
        memberId,
        "CAMPAIGN_CLOSED",
        "CAMPAIGN",
        campaignId,
        "{\"open\":true}",
        "{\"open\":false}",
        null);
    return CampaignManagementDtos.DetailResponse.from(campaign);
  }

  @Transactional
  public CampaignManagementDtos.ContactResponse saveContact(
      Long memberId, Long campaignId, CampaignManagementDtos.ContactSaveRequest request) {
    Campaign campaign = getOwnedCampaign(memberId, campaignId);
    campaign.updateContact(request.name().trim(), request.phone().trim());
    auditLogService.record(
        memberId, "CAMPAIGN_CONTACT_UPDATED", "CAMPAIGN", campaignId, null, null, null);
    return new CampaignManagementDtos.ContactResponse(
        campaign.getContactName(), campaign.getContactPhone());
  }

  public CampaignManagementDtos.ContactResponse getContact(Long memberId, Long campaignId) {
    Campaign campaign = getOwnedCampaign(memberId, campaignId);
    if (campaign.getContactName() == null || campaign.getContactPhone() == null) return null;
    return new CampaignManagementDtos.ContactResponse(
        campaign.getContactName(), campaign.getContactPhone());
  }

  /** 신청 이력이 생긴 뒤에는 감사 추적을 위해 삭제하지 않고 종료만 허용합니다. */
  @Transactional
  public void delete(Long memberId, Long campaignId) {
    Campaign campaign = getOwnedCampaign(memberId, campaignId);
    if (applicationRepository.existsByCampaignId(campaignId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "신청 이력이 있는 캠페인은 삭제할 수 없습니다. 모집을 종료해 주세요.");
    }
    formRepository.findByCampaignId(campaignId).ifPresent(formRepository::delete);
    campaignRepository.delete(campaign);
    auditLogService.record(memberId, "CAMPAIGN_DELETED", "CAMPAIGN", campaignId, null, null, null);
  }

  private Campaign getOwnedCampaign(Long memberId, Long campaignId) {
    Campaign campaign = getCampaign(campaignId);
    productService.getOwnedProduct(memberId, campaign.getPreorder().getProduct().getId());
    return campaign;
  }

  private Campaign getCampaign(Long campaignId) {
    return campaignRepository
        .findById(campaignId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
