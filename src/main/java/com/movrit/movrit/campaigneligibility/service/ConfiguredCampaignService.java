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

/** 업체 캠페인 작성 화면의 '한 번에 저장'을 처리합니다. 캠페인 본문·신청서·신청 조건·이미지를 하나의 트랜잭션으로 묶어, 일부만 저장된 캠페인이 생기지 않게 합니다. */
@Service
@RequiredArgsConstructor
public class ConfiguredCampaignService {
  private final CollaborationService collaborationService;
  private final CampaignManagementService campaignManagementService;
  private final CampaignEligibilityPolicyService eligibilityPolicyService;
  private final CampaignApplicationService campaignApplicationService;
  private final CampaignMediaService campaignMediaService;

  /**
   * 새 캠페인을 본문 → 신청서 → 신청 조건(선택 시) → 이미지 순서로 한 번에 등록합니다. 중간에 하나라도 실패하면 전부 취소되어 조건만 빠진 공개 캠페인이 생기지
   * 않습니다.
   */
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

  /**
   * 기존 캠페인의 본문·신청서·신청 조건·이미지를 한 번에 수정합니다.
   *
   * <p>저장 전에 이미지 변경이 가능한지 먼저 확인하고, 각 항목은 화면이 보낸 기대 버전과 비교해 다른 화면의 수정을 덮어쓰지 않습니다. 조건을 '없음'으로 바꾸면 기존
   * 조건을 해제하며, 하나라도 실패하면 모든 변경을 되돌립니다.
   */
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
        // 화면이 조건이 없다고 알고 있는데 서버에는 조건이 있으면, 다른 화면에서 추가된 것이므로 거절합니다.
        if (request.expectedRequirementVersion() == null) conflict();
        eligibilityPolicyService.delete(memberId, campaignId, request.expectedRequirementVersion());
      }
      requirement = null;
    } else {
      // 새 조건으로 저장하려는데 이미 조건이 생겨 있다면 다른 화면의 변경을 덮어쓰지 않도록 거절합니다.
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

  /** '단일 조건'을 고른 경우에만 조건 내용이 있어야 하고, '조건 없음'이면 조건 내용이 없어야 합니다. */
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
