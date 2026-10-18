package com.movrit.movrit.collaboration.controller;

import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.service.CollaborationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collaborations")
public class CollaborationController {
  private final CollaborationService s;

  @PostMapping("/campaigns")
  public ResponseEntity<CollaborationDtos.CampaignResponse> campaign(
      @AuthenticationPrincipal Jwt j, @Valid @RequestBody CollaborationDtos.CampaignRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(s.createCampaign(id(j), r));
  }

  @GetMapping("/campaigns/open")
  public List<CollaborationDtos.CampaignResponse> open() {
    return s.openCampaigns();
  }

  @GetMapping("/campaigns/seller")
  public List<CollaborationDtos.CampaignResponse> sellerCampaigns(@AuthenticationPrincipal Jwt j) {
    return s.sellerCampaigns(id(j));
  }

  @PostMapping("/campaigns/{campaignId}/apply")
  public CollaborationDtos.Response apply(
      @AuthenticationPrincipal Jwt j,
      @PathVariable Long campaignId,
      @Valid @RequestBody CollaborationDtos.ApplyRequest r) {
    return s.apply(id(j), campaignId, r);
  }

  @PostMapping("/proposals")
  public CollaborationDtos.Response propose(
      @AuthenticationPrincipal Jwt j, @Valid @RequestBody CollaborationDtos.ProposeRequest r) {
    return s.propose(id(j), r);
  }

  @GetMapping("/mine")
  public List<CollaborationDtos.Response> mine(@AuthenticationPrincipal Jwt j) {
    return s.mine(id(j));
  }

  @GetMapping("/seller")
  public List<CollaborationDtos.Response> seller(@AuthenticationPrincipal Jwt j) {
    return s.seller(id(j));
  }

  @PatchMapping("/{id}/approve")
  public CollaborationDtos.Response approve(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.sellerDecision(id(j), id, true);
  }

  @PatchMapping("/{id}/reject")
  public CollaborationDtos.Response reject(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.sellerDecision(id(j), id, false);
  }

  @PatchMapping("/{id}/accept")
  public CollaborationDtos.Response accept(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.creatorDecision(id(j), id, true);
  }

  @PatchMapping("/{id}/decline")
  public CollaborationDtos.Response decline(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.creatorDecision(id(j), id, false);
  }

  @PatchMapping("/{id}/end")
  public CollaborationDtos.Response end(@AuthenticationPrincipal Jwt j, @PathVariable Long id) {
    return s.end(id(j), id);
  }

  @PostMapping("/referrals/{code}/click")
  public ResponseEntity<Void> click(
      @PathVariable String code, @RequestBody CollaborationDtos.ClickRequest r) {
    s.click(code, r);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/performance/mine")
  public List<CollaborationDtos.PerformanceResponse> myPerformance(@AuthenticationPrincipal Jwt j) {
    return s.creatorPerformance(id(j));
  }

  @GetMapping("/performance/seller")
  public List<CollaborationDtos.PerformanceResponse> sellerPerformance(
      @AuthenticationPrincipal Jwt j) {
    return s.sellerPerformance(id(j));
  }

  private Long id(Jwt j) {
    return Long.valueOf(j.getSubject());
  }
}
