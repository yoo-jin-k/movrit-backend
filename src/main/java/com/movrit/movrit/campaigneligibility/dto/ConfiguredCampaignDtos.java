package com.movrit.movrit.campaigneligibility.dto;

import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public final class ConfiguredCampaignDtos {
    private ConfiguredCampaignDtos() {}

    public enum RequirementMode { NONE, SINGLE }

    public record CreateRequest(
            @NotNull @Valid CollaborationDtos.CampaignRequest campaign,
            @NotNull RequirementMode requirementMode,
            @Valid CampaignEligibilityDtos.RequirementSaveRequest requirement
    ) {}

    public record UpdateRequest(
            @NotNull @Valid CampaignManagementDtos.UpdateRequest campaign,
            @NotNull RequirementMode requirementMode,
            @Valid CampaignEligibilityDtos.RequirementSaveRequest requirement,
            Long expectedRequirementVersion
    ) {}

    public record CreateResponse(
            CollaborationDtos.CampaignResponse campaign,
            CampaignEligibilityDtos.RequirementResponse requirement
    ) {}

    public record UpdateResponse(
            CampaignManagementDtos.DetailResponse campaign,
            CampaignEligibilityDtos.RequirementResponse requirement
    ) {}
}
