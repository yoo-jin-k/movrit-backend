package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignmedia.service.CampaignPublicPolicy;
import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignSelectionMode;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 업체가 자기 캠페인을 수정·마감·삭제하고 담당자 연락처를 관리하는 기능을 맡습니다. 모든 변경은 캠페인 소유 업체만 할 수 있고, 변경 내역은 감사 로그에 남깁니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignManagementService {
  private final CampaignRepository campaignRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final CampaignApplicationFormRepository formRepository;
  private final CampaignOwnershipService campaignOwnership;
  private final AuditLogService auditLogService;

  /** 사용자에게 공개된 캠페인 상세를 조회합니다. 공개 조건을 만족하지 않으면 찾을 수 없음으로 응답하고, 신청서에 방문형이 있으면 방문 정보를 함께 보여줍니다. */
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

  /**
   * 업체가 캠페인 모집 내용과 일정을 수정합니다.
   *
   * <p>거절되는 경우: 소유 업체가 아님(403), 다른 화면에서 일정이 먼저 바뀜(409), 신청 이력이 생긴 뒤 선정 방식 변경(409), 일정 순서 오류(400).
   */
  @Transactional
  public CampaignManagementDtos.DetailResponse update(
      Long memberId, Long campaignId, CampaignManagementDtos.UpdateRequest request) {
    Campaign campaign = getOwnedCampaign(memberId, campaignId);
    if (!java.util.Objects.equals(
        campaign.getScheduleVersion(), request.expectedScheduleVersion())) {
      // 화면에서 불러온 일정 버전이 오래되었으면 최신 내용을 덮어쓰지 않도록 저장 전에 막습니다.
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
      // 유형·지역·주요 채널은 보낸 값만 바꾸고, 비운 값은 기존 분류를 유지합니다.
      campaign.classify(request.campaignType(), request.region(), request.contentChannel());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
    auditLogService.record(memberId, "CAMPAIGN_UPDATED", "CAMPAIGN", campaignId, null, null, null);
    return CampaignManagementDtos.DetailResponse.from(campaign);
  }

  /** 업체가 캠페인 모집을 마감해 사용자 목록에서 내립니다. 기존 신청 데이터는 그대로 유지합니다. */
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

  /** 선정된 참여자에게 보여줄 캠페인 담당자 이름·연락처를 저장합니다. 다시 저장하면 새 값으로 바뀝니다. */
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

  /** 업체가 저장한 담당자 연락처를 다시 조회합니다. 아직 입력하지 않았으면 null을 반환합니다. */
  public CampaignManagementDtos.ContactResponse getContact(Long memberId, Long campaignId) {
    Campaign campaign = getOwnedCampaign(memberId, campaignId);
    if (campaign.getContactName() == null || campaign.getContactPhone() == null) return null;
    return new CampaignManagementDtos.ContactResponse(
        campaign.getContactName(), campaign.getContactPhone());
  }

  /** 업체가 캠페인과 연결된 신청서를 삭제합니다. 신청 이력이 하나라도 있으면 기록 보존을 위해 삭제하지 않고 마감만 하도록 안내합니다(409). */
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

  /** 캠페인을 찾고, 요청한 회원이 그 캠페인 상품의 소유 업체인지 확인합니다. */
  private Campaign getOwnedCampaign(Long memberId, Long campaignId) {
    Campaign campaign = getCampaign(campaignId);
    campaignOwnership.requireOwner(memberId, campaign);
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
