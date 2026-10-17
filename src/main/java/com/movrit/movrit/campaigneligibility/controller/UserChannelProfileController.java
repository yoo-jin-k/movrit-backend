package com.movrit.movrit.campaigneligibility.controller;

import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.service.MemberChannelMetricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/channel-profile")
public class UserChannelProfileController {
    private final MemberChannelMetricService service;

    @GetMapping
    public CampaignEligibilityDtos.ChannelProfileResponse mine(@AuthenticationPrincipal Jwt jwt) {
        return service.findMine(memberId(jwt));
    }

    @PutMapping
    public CampaignEligibilityDtos.ChannelProfileResponse save(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CampaignEligibilityDtos.MetricSaveRequest request
    ) {
        return service.saveMine(memberId(jwt), request);
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
