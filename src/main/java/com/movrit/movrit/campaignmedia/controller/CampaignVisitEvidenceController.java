package com.movrit.movrit.campaignmedia.controller;
import com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService;
import com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor
public class CampaignVisitEvidenceController {
    private final CampaignVisitEvidenceService service;
    @GetMapping("/api/campaign-applications/{applicationId}/visit-snapshot")
    public SnapshotView snapshot(@AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
        return service.snapshot(Long.valueOf(jwt.getSubject()), applicationId);
    }
    @GetMapping("/api/campaigns/{campaignId}/visit-history")
    public List<HistoryView> history(@AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
        return service.history(Long.valueOf(jwt.getSubject()), campaignId);
    }
}
