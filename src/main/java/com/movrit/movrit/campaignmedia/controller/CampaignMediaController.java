package com.movrit.movrit.campaignmedia.controller;

import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.AssetResponse;
import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.ManagedPresentation;
import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.PublicPresentation;
import com.movrit.movrit.campaignmedia.service.CampaignMediaService;
import com.movrit.movrit.common.security.JwtMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 캠페인 이미지와 방문 주소의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
public class CampaignMediaController {
  private final CampaignMediaService service;

  /** 이미지 자산 파일을 업로드합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @PostMapping("/api/campaign-image-assets")
  public ResponseEntity<AssetResponse> upload(
      @AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(member(jwt), file));
  }

  /** 로그인 회원의 접근 권한을 서비스에서 확인한 이미지 미리보기를 반환합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/api/campaign-image-assets/{assetId}/preview")
  public ResponseEntity<Resource> preview(
      @AuthenticationPrincipal Jwt jwt, @PathVariable String assetId) {
    return image(service.preview(member(jwt), assetId));
  }

  /** 공개 가능한 이미지 본문을 반환합니다. */
  @GetMapping("/api/campaign-image-assets/{assetId}/content")
  public ResponseEntity<Resource> content(@PathVariable String assetId) {
    return image(service.content(assetId));
  }

  /** 업체 관리용 이미지·주소와 버전을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/api/campaigns/{campaignId}/presentation/manage")
  public ManagedPresentation manage(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long campaignId) {
    return service.manage(member(jwt), campaignId);
  }

  /** 사용자 화면에 공개할 이미지·주소를 조회합니다. */
  @GetMapping("/api/campaigns/{campaignId}/presentation/public")
  public PublicPresentation publicPresentation(@PathVariable Long campaignId) {
    return service.publicPresentation(campaignId);
  }

  /** 검증된 이미지 파일을 반환합니다. 캐시 저장과 MIME 추측을 차단하여 권한 변경 후 캐시 노출과 유형 오인을 줄입니다. */
  private ResponseEntity<Resource> image(CampaignMediaService.ImageFile file) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .body(new FileSystemResource(file.path()));
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long member(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
