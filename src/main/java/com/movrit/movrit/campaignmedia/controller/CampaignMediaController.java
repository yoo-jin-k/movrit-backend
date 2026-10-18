package com.movrit.movrit.campaignmedia.controller;

import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.*;
import com.movrit.movrit.campaignmedia.service.CampaignMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class CampaignMediaController {
  private final CampaignMediaService service;

  @PostMapping("/api/campaign-image-assets")
  public ResponseEntity<AssetResponse> upload(
      @AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(member(jwt), file));
  }

  @GetMapping("/api/campaign-image-assets/{assetId}/preview")
  public ResponseEntity<Resource> preview(
      @AuthenticationPrincipal Jwt jwt, @PathVariable String assetId) {
    return image(service.preview(member(jwt), assetId));
  }

  @GetMapping("/api/campaign-image-assets/{assetId}/content")
  public ResponseEntity<Resource> content(@PathVariable String assetId) {
    return image(service.content(assetId));
  }

  @GetMapping("/api/campaigns/{campaignId}/presentation/manage")
  public ManagedPresentation manage(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.manage(member(jwt), campaignId);
  }

  @GetMapping("/api/campaigns/{campaignId}/presentation/public")
  public PublicPresentation publicPresentation(@PathVariable Long campaignId) {
    return service.publicPresentation(campaignId);
  }

  private ResponseEntity<Resource> image(CampaignMediaService.ImageFile file) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .body(new FileSystemResource(file.path()));
  }

  private Long member(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
