package com.movrit.movrit.creator.controller;

import com.movrit.movrit.creator.dto.CreatorDtos;
import com.movrit.movrit.creator.service.CreatorService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 채널 프로필의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/creators")
public class CreatorController {
  private final CreatorService service;

  /** 채널 프로필 관련 생성을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PostMapping
  public ResponseEntity<CreatorDtos.Response> create(
      @AuthenticationPrincipal Jwt j, @Valid @RequestBody CreatorDtos.SaveRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(id(j), r));
  }

  /** 채널 프로필 관련 로그인 회원에게 연결된 정보를 조회합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @GetMapping("/me")
  public CreatorDtos.Response mine(@AuthenticationPrincipal Jwt j) {
    return service.mine(id(j));
  }

  /** 채널 프로필 관련 관리 대상 목록을 조회합니다. */
  @GetMapping
  public List<CreatorDtos.Response> all() {
    return service.findAll();
  }

  /** 채널 프로필 관련 단일 프로필을 조회합니다. */
  @GetMapping("/{id}")
  public CreatorDtos.Response one(@PathVariable Long id) {
    return service.findById(id);
  }

  /** 채널 프로필 관련 수정을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PutMapping("/me")
  public CreatorDtos.Response update(
      @AuthenticationPrincipal Jwt j, @Valid @RequestBody CreatorDtos.SaveRequest r) {
    return service.update(id(j), r);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long id(Jwt j) {
    return Long.valueOf(j.getSubject());
  }
}
