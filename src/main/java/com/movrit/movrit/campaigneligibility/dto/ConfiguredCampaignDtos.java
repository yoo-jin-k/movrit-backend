package com.movrit.movrit.campaigneligibility.dto;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos;
import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public final class ConfiguredCampaignDtos {
  private ConfiguredCampaignDtos() {}

  public enum RequirementMode {
    NONE,
    SINGLE
  }

  public record CreateRequest(
      @NotNull @Valid CollaborationDtos.CampaignRequest campaign,
      @NotNull @Valid CampaignApplicationDtos.FormSaveRequest applicationForm,
      @NotNull RequirementMode requirementMode,
      @Valid CampaignEligibilityDtos.RequirementSaveRequest requirement,
      @NotNull @Valid CampaignMediaDtos.PresentationInput presentation) {}

  public record UpdateRequest(
      @NotNull @Valid CampaignManagementDtos.UpdateRequest campaign,
      @Valid CampaignApplicationDtos.FormSaveRequest applicationForm,
      Long expectedApplicationFormVersion,
      @NotNull RequirementMode requirementMode,
      @Valid CampaignEligibilityDtos.RequirementSaveRequest requirement,
      Long expectedRequirementVersion,
      @Valid CampaignMediaDtos.PresentationInput presentation,
      Long expectedPresentationVersion) {}

  public record CreateResponse(
      CollaborationDtos.CampaignResponse campaign,
      CampaignApplicationDtos.FormResponse applicationForm,
      CampaignEligibilityDtos.RequirementResponse requirement,
      CampaignMediaDtos.ManagedPresentation presentation) {}

  public record UpdateResponse(
      CampaignManagementDtos.DetailResponse campaign,
      CampaignApplicationDtos.FormResponse applicationForm,
      CampaignEligibilityDtos.RequirementResponse requirement,
      CampaignMediaDtos.ManagedPresentation presentation) {}
}
