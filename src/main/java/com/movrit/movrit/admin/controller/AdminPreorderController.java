package com.movrit.movrit.admin.controller;

import com.movrit.movrit.admin.service.AdminPreorderService;
import com.movrit.movrit.preorder.dto.PreorderDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/preorders")
public class AdminPreorderController {

    private final AdminPreorderService service;

    @GetMapping("/reviews")
    public List<PreorderDtos.Response> reviews(@AuthenticationPrincipal Jwt jwt) {
        return service.reviewQueue(id(jwt));
    }

    @GetMapping("/cancellations")
    public List<PreorderDtos.Response> cancellations(@AuthenticationPrincipal Jwt jwt) {
        return service.cancellationQueue(id(jwt));
    }

    @PatchMapping("/{preorderId}/review/approve")
    public PreorderDtos.Response approveReview(@AuthenticationPrincipal Jwt jwt, @PathVariable Long preorderId) {
        return service.approveReview(id(jwt), preorderId);
    }

    @PatchMapping("/{preorderId}/review/changes")
    public PreorderDtos.Response requestChanges(@AuthenticationPrincipal Jwt jwt, @PathVariable Long preorderId) {
        return service.requestChanges(id(jwt), preorderId);
    }

    @PatchMapping("/{preorderId}/cancel/approve")
    public PreorderDtos.Response approveCancellation(@AuthenticationPrincipal Jwt jwt, @PathVariable Long preorderId) {
        return service.approveCancellation(id(jwt), preorderId);
    }

    @PatchMapping("/{preorderId}/cancel/reject")
    public PreorderDtos.Response rejectCancellation(@AuthenticationPrincipal Jwt jwt, @PathVariable Long preorderId) {
        return service.rejectCancellation(id(jwt), preorderId);
    }

    private Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
