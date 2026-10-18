package com.movrit.movrit.campaigneligibility.dto;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos;
import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** 캠페인 참여 조건의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class ConfiguredCampaignDtos {
  private ConfiguredCampaignDtos() {}

  /** 업체가 캠페인별로 조건 없음 또는 단일 채널 지표 조건을 선택합니다. 전역 팔로워 기준이 아닙니다. */
  public enum RequirementMode {
    NONE,
    SINGLE
  }

  /**
   * 캠페인 참여 조건의 생성 요청을 전달합니다.
   *
   * @param campaign 캠페인 기본 정보입니다. null을 허용하지 않습니다.
   * @param applicationForm 캠페인 신청서 구성입니다. null을 허용하지 않습니다.
   * @param requirementMode NONE은 조건 없음, SINGLE은 업체가 지정한 단일 지표 조건입니다. null을 허용하지 않습니다.
   * @param requirement 캠페인별 지표 조건이며 조건 없음 모드에서는 null입니다.
   * @param presentation 이미지 및 방문 주소 표시 정보입니다. null을 허용하지 않습니다.
   */
  public record CreateRequest(
      @NotNull @Valid CollaborationDtos.CampaignRequest campaign,
      @NotNull @Valid CampaignApplicationDtos.FormSaveRequest applicationForm,
      @NotNull RequirementMode requirementMode,
      @Valid CampaignEligibilityDtos.RequirementSaveRequest requirement,
      @NotNull @Valid CampaignMediaDtos.PresentationInput presentation) {}

  /**
   * 캠페인 참여 조건의 수정 요청을 전달합니다.
   *
   * @param campaign 캠페인 기본 정보입니다. null을 허용하지 않습니다.
   * @param applicationForm 캠페인 신청서 구성입니다.
   * @param expectedApplicationFormVersion 수정하려는 신청서의 기존 버전입니다.
   * @param requirementMode NONE은 조건 없음, SINGLE은 업체가 지정한 단일 지표 조건입니다. null을 허용하지 않습니다.
   * @param requirement 캠페인별 지표 조건이며 조건 없음 모드에서는 null입니다.
   * @param expectedRequirementVersion 기존 참여 조건의 버전이며 조건 삭제 시에도 사용합니다.
   * @param presentation 이미지 및 방문 주소 표시 정보입니다.
   * @param expectedPresentationVersion 이미지·방문 주소를 수정하기 전에 조회한 표시 정보 버전입니다.
   */
  public record UpdateRequest(
      @NotNull @Valid CampaignManagementDtos.UpdateRequest campaign,
      @Valid CampaignApplicationDtos.FormSaveRequest applicationForm,
      Long expectedApplicationFormVersion,
      @NotNull RequirementMode requirementMode,
      @Valid CampaignEligibilityDtos.RequirementSaveRequest requirement,
      Long expectedRequirementVersion,
      @Valid CampaignMediaDtos.PresentationInput presentation,
      Long expectedPresentationVersion) {}

  /**
   * 캠페인 참여 조건의 생성 결과를 전달합니다.
   *
   * @param campaign 캠페인 기본 정보입니다.
   * @param applicationForm 캠페인 신청서 구성입니다.
   * @param requirement 캠페인별 지표 조건이며 조건 없음 모드에서는 null입니다.
   * @param presentation 이미지 및 방문 주소 표시 정보입니다.
   */
  public record CreateResponse(
      CollaborationDtos.CampaignResponse campaign,
      CampaignApplicationDtos.FormResponse applicationForm,
      CampaignEligibilityDtos.RequirementResponse requirement,
      CampaignMediaDtos.ManagedPresentation presentation) {}

  /**
   * 캠페인 참여 조건의 수정 결과를 전달합니다.
   *
   * @param campaign 캠페인 기본 정보입니다.
   * @param applicationForm 캠페인 신청서 구성입니다.
   * @param requirement 캠페인별 지표 조건이며 조건 없음 모드에서는 null입니다.
   * @param presentation 이미지 및 방문 주소 표시 정보입니다.
   */
  public record UpdateResponse(
      CampaignManagementDtos.DetailResponse campaign,
      CampaignApplicationDtos.FormResponse applicationForm,
      CampaignEligibilityDtos.RequirementResponse requirement,
      CampaignMediaDtos.ManagedPresentation presentation) {}
}
