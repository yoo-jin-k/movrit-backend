package com.movrit.movrit.memberavatar.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.member.dto.MemberSignupResponse;
import com.movrit.movrit.memberavatar.service.MemberAvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 회원 프로필 사진 API입니다. 사진 올리기·삭제는 본인만, 사진 보기는 누구나 할 수 있습니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberAvatarController {
  private final MemberAvatarService service;

  /** 인증된 본인의 사진만 등록하거나 교체합니다. 요청 파일명은 저장에 사용하지 않습니다. */
  @PutMapping("/me/avatar")
  public MemberSignupResponse replace(
      @AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
    return service.replace(JwtMemberId.from(jwt), file);
  }

  /** 인증된 본인의 사진만 삭제합니다. */
  @DeleteMapping("/me/avatar")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
    service.delete(JwtMemberId.from(jwt));
    return ResponseEntity.noContent().build();
  }

  /** 버전이 현재 DB 값과 일치하는 재인코딩 표시본만 공개합니다. */
  @GetMapping("/{memberId}/avatar")
  public ResponseEntity<Resource> content(
      @PathVariable Long memberId, @RequestParam("v") long version) {
    MemberAvatarService.AvatarFile avatar = service.findPublic(memberId, version);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(avatar.contentType()))
        .contentLength(avatar.path().toFile().length())
        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        .header(HttpHeaders.PRAGMA, "no-cache")
        .header(HttpHeaders.EXPIRES, "0")
        .header("X-Content-Type-Options", "nosniff")
        .eTag("\"avatar-" + memberId + "-" + avatar.version() + "\"")
        .body(new FileSystemResource(avatar.path()));
  }
}
