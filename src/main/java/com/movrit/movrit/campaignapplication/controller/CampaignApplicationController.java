package com.movrit.movrit.campaignapplication.controller;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.service.CampaignApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns")
public class CampaignApplicationController {
    private final CampaignApplicationService service;

    @PutMapping("/{campaignId}/application-form")
    public CampaignApplicationDtos.FormResponse saveForm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long campaignId,
            @Valid @RequestBody CampaignApplicationDtos.FormSaveRequest request
    ) {
        return service.saveForm(memberId(jwt), campaignId, request);
    }

    @GetMapping("/{campaignId}/application-form")
    public CampaignApplicationDtos.FormResponse getForm(@PathVariable Long campaignId) {
        return service.getForm(campaignId);
    }

    @GetMapping("/{campaignId}/eligibility")
    public CampaignApplicationDtos.EligibilityResponse eligibility(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long campaignId
    ) {
        return service.getEligibility(memberId(jwt), campaignId);
    }

    @PostMapping("/{campaignId}/applications")
    public ResponseEntity<CampaignApplicationDtos.Response> apply(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long campaignId,
            @Valid @RequestBody CampaignApplicationDtos.CreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.apply(memberId(jwt), campaignId, request));
    }

    @GetMapping("/applications/mine")
    public List<CampaignApplicationDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.findMine(memberId(jwt));
    }

    @GetMapping("/applications/seller")
    public List<CampaignApplicationDtos.Response> seller(@AuthenticationPrincipal Jwt jwt) {
        return service.findSellerApplications(memberId(jwt));
    }

    @PatchMapping("/applications/{applicationId}/select")
    public CampaignApplicationDtos.Response select(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long applicationId
    ) {
        return service.select(memberId(jwt), applicationId);
    }

    @PatchMapping("/applications/{applicationId}/reject")
    public CampaignApplicationDtos.Response reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long applicationId
    ) {
        return service.reject(memberId(jwt), applicationId);
    }

    @PatchMapping("/applications/{applicationId}/activate")
    public CampaignApplicationDtos.Response activate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long applicationId
    ) {
        return service.activate(memberId(jwt), applicationId);
    }

    @PatchMapping("/applications/{applicationId}/cancel")
    public CampaignApplicationDtos.Response cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long applicationId,
            @Valid @RequestBody CampaignApplicationDtos.CancelRequest request
    ) {
        return service.cancel(memberId(jwt), applicationId, request);
    }

    @PatchMapping("/applications/{applicationId}/experience-confirm")
    public CampaignApplicationDtos.Response confirmExperience(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long applicationId
    ) {
        return service.confirmExperience(memberId(jwt), applicationId);
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
