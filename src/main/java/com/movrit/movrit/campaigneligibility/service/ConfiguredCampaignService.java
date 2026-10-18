package com.movrit.movrit.campaigneligibility.service;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.service.CampaignApplicationService;
import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.dto.ConfiguredCampaignDtos;
import com.movrit.movrit.campaignmedia.service.CampaignMediaService;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.service.CampaignManagementService;
import com.movrit.movrit.collaboration.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConfiguredCampaignService {
  private final CollaborationService collaborationService;
  private final CampaignManagementService campaignManagementService;
  private final CampaignEligibilityPolicyService eligibilityPolicyService;
  private final CampaignApplicationService campaignApplicationService;
  private final CampaignMediaService campaignMediaService;

  /** 캠페인과 조건을 한 트랜잭션에서 저장해 조건만 누락된 공개 캠페인이 생기지 않게 합니다. */
  @Transactional
  public ConfiguredCampaignDtos.CreateResponse create(
      Long memberId, ConfiguredCampaignDtos.CreateRequest request) {
    validateMode(request.requirementMode(), request.requirement());
    CollaborationDtos.CampaignResponse campaign =
        collaborationService.createCampaign(memberId, request.campaign());
    CampaignApplicationDtos.FormResponse applicationForm =
        campaignApplicationService.saveForm(memberId, campaign.id(), request.applicationForm());
    CampaignEligibilityDtos.RequirementResponse requirement =
        request.requirementMode() == ConfiguredCampaignDtos.RequirementMode.SINGLE
            ? eligibilityPolicyService.save(memberId, campaign.id(), request.requirement())
            : null;
    var presentation =
        campaignMediaService.apply(memberId, campaign.id(), request.presentation(), null, true);
    return new ConfiguredCampaignDtos.CreateResponse(
        campaign, applicationForm, requirement, presentation);
  }

  /** 캠페인 본문과 조건 변경 중 하나라도 실패하면 둘 다 이전 상태로 되돌립니다. */
  @Transactional
  public ConfiguredCampaignDtos.UpdateResponse update(
      Long memberId, Long campaignId, ConfiguredCampaignDtos.UpdateRequest request) {
    validateMode(request.requirementMode(), request.requirement());
    campaignMediaService.preflightUpdate(
        memberId,
        campaignId,
        request.presentation(),
        request.expectedPresentationVersion(),
        request.applicationForm());
    var campaign = campaignManagementService.update(memberId, campaignId, request.campaign());
    CampaignApplicationDtos.FormResponse applicationForm =
        request.applicationForm() == null
            ? campaignApplicationService.getForm(campaignId)
            : campaignApplicationService.saveFormWithExpectedVersion(
                memberId,
                campaignId,
                request.applicationForm(),
                request.expectedApplicationFormVersion());
    CampaignEligibilityDtos.RequirementResponse requirement;
    if (request.requirementMode() == ConfiguredCampaignDtos.RequirementMode.NONE) {
      CampaignEligibilityDtos.RequirementResponse current =
          eligibilityPolicyService.find(campaignId);
      if (current != null) {
        if (request.expectedRequirementVersion() == null) conflict();
        eligibilityPolicyService.delete(memberId, campaignId, request.expectedRequirementVersion());
      }
      requirement = null;
    } else {
      if (request.requirement().expectedVersion() == null
          && eligibilityPolicyService.find(campaignId) != null) conflict();
      requirement = eligibilityPolicyService.save(memberId, campaignId, request.requirement());
    }
    var presentation =
        request.presentation() == null
            ? campaignMediaService.manage(memberId, campaignId)
            : campaignMediaService.apply(
                memberId,
                campaignId,
                request.presentation(),
                request.expectedPresentationVersion(),
                false);
    if (request.presentation() != null) campaign = campaignManagementService.getDetail(campaignId);
    return new ConfiguredCampaignDtos.UpdateResponse(
        campaign, applicationForm, requirement, presentation);
  }

  private void validateMode(
      ConfiguredCampaignDtos.RequirementMode mode,
      CampaignEligibilityDtos.RequirementSaveRequest requirement) {
    if ((mode == ConfiguredCampaignDtos.RequirementMode.SINGLE) != (requirement != null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단일 조건을 선택한 경우에만 캠페인 조건을 입력해 주세요.");
    }
  }

  private void conflict() {
    throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 조건이 변경되었습니다. 다시 조회해 주세요.");
  }
}
