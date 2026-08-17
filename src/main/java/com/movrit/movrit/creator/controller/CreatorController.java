package com.movrit.movrit.creator.controller;

import com.movrit.movrit.creator.dto.CreatorDtos;
import com.movrit.movrit.creator.entity.CreatorStatus;
import com.movrit.movrit.creator.service.CreatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/creators")
public class CreatorController {
    private final CreatorService creatorService;

    @PostMapping("/apply")
    public ResponseEntity<CreatorDtos.Response> apply(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatorDtos.ApplyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creatorService.apply(memberId(jwt), request));
    }

    @GetMapping("/me")
    public CreatorDtos.Response findMine(@AuthenticationPrincipal Jwt jwt) {
        return creatorService.findMine(memberId(jwt));
    }

    @PutMapping("/me")
    public CreatorDtos.Response updateMine(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatorDtos.UpdateProfileRequest request
    ) {
        return creatorService.updateMine(memberId(jwt), request);
    }

    @GetMapping("/me/dashboard")
    public CreatorDtos.DashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt) {
        return creatorService.dashboard(memberId(jwt));
    }

    @PatchMapping("/me/campaigns/{campaignId}/status")
    public CreatorDtos.CampaignResponse updateCampaignStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long campaignId,
            @Valid @RequestBody CreatorDtos.CampaignStatusRequest request
    ) {
        return creatorService.updateCampaignStatus(memberId(jwt), campaignId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<CreatorDtos.Response> findAll(@RequestParam(required = false) CreatorStatus status) {
        return creatorService.findAll(status);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public CreatorDtos.Response findById(@PathVariable Long id) {
        return creatorService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/approve")
    public CreatorDtos.Response approve(@PathVariable Long id) {
        return creatorService.approve(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reject")
    public CreatorDtos.Response reject(
            @PathVariable Long id,
            @Valid @RequestBody CreatorDtos.RejectRequest request
    ) {
        return creatorService.reject(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/campaigns")
    public ResponseEntity<CreatorDtos.CampaignResponse> assignCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CreatorDtos.AssignCampaignRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creatorService.assignCampaign(id, request));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
