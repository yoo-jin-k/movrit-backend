package com.movrit.movrit.campaigneligibility.dto;

import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public final class ConfiguredCampaignDtos {
    private ConfiguredCampaignDtos() {}

    public enum RequirementMode { NONE, SINGLE }

    public record CreateRequest(
            @NotNull @Valid CollaborationDtos.CampaignRequest campaign,
            @NotNull @Valid CampaignApplicationDtos.FormSaveRequest applicationForm,
            @NotNull RequirementMode requirementMode,
            @Valid CampaignEligibilityDtos.RequirementSaveRequest requirement
    ) {}

    public record UpdateRequest(
            @NotNull @Valid CampaignManagementDtos.UpdateRequest campaign,
            @Valid CampaignApplicationDtos.FormSaveRequest applicationForm,
            @NotNull RequirementMode requirementMode,
            @Valid CampaignEligibilityDtos.RequirementSaveRequest requirement,
            Long expectedRequirementVersion
    ) {}

    public record CreateResponse(
            CollaborationDtos.CampaignResponse campaign,
            CampaignApplicationDtos.FormResponse applicationForm,
            CampaignEligibilityDtos.RequirementResponse requirement
    ) {}

    public record UpdateResponse(
            CampaignManagementDtos.DetailResponse campaign,
            CampaignApplicationDtos.FormResponse applicationForm,
            CampaignEligibilityDtos.RequirementResponse requirement
    ) {}
}
