package com.movrit.movrit.notice.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.notice.dto.NoticeDtos;
import com.movrit.movrit.notice.service.NoticeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공지사항 API입니다.
 *
 * <ul>
 *   <li>누구나: GET /api/notices, GET /api/notices/{id} (게시된 공지만)
 *   <li>운영자: GET·POST /api/admin/notices, PUT·DELETE /api/admin/notices/{id}
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class NoticeController {
  private final NoticeService service;

  /** 게시된 공지 목록입니다. 로그인하지 않아도 볼 수 있습니다. */
  @GetMapping("/api/notices")
  public List<NoticeDtos.Response> published() {
    return service.published();
  }

  /** 게시된 공지 한 건입니다. */
  @GetMapping("/api/notices/{id}")
  public NoticeDtos.Response publishedOne(@PathVariable Long id) {
    return service.publishedOne(id);
  }

  /** 운영자 공지 목록입니다(게시 전 포함). */
  @GetMapping("/api/admin/notices")
  public List<NoticeDtos.Response> all(@AuthenticationPrincipal Jwt jwt) {
    return service.all(JwtMemberId.from(jwt));
  }

  /** 공지를 새로 씁니다. 성공하면 201입니다. */
  @PostMapping("/api/admin/notices")
  public ResponseEntity<NoticeDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NoticeDtos.SaveRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(JwtMemberId.from(jwt), request));
  }

  /** 공지를 고칩니다. */
  @PutMapping("/api/admin/notices/{id}")
  public NoticeDtos.Response update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @Valid @RequestBody NoticeDtos.SaveRequest request) {
    return service.update(JwtMemberId.from(jwt), id, request);
  }

  /** 공지를 지웁니다. */
  @DeleteMapping("/api/admin/notices/{id}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    service.delete(JwtMemberId.from(jwt), id);
    return ResponseEntity.noContent().build();
  }
}
