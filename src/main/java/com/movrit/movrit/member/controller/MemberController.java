package com.movrit.movrit.member.controller;

import com.movrit.movrit.member.dto.*;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.service.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 회원의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
  private final MemberService service;

  /** 회원 관련 일반 회원가입을 요청합니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PostMapping("/signup")
  public ResponseEntity<MemberSignupResponse> signup(@Valid @RequestBody MemberSignupRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.signup(r));
  }

  /** 회원 관련 로그인 회원 정보를 조회합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @GetMapping("/me")
  public MemberSignupResponse me(@AuthenticationPrincipal Jwt j) {
    return service.findById(id(j));
  }

  /** 회원 관련 수정을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PutMapping("/me")
  public MemberSignupResponse update(
      @AuthenticationPrincipal Jwt j, @Valid @RequestBody MemberUpdateRequest r) {
    return service.update(id(j), r);
  }

  /** 회원 관련 로그인 회원 탈퇴를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @DeleteMapping("/me")
  public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Jwt j) {
    service.delete(id(j));
    return ResponseEntity.noContent().build();
  }

  /** 회원 관련 관리 대상 목록을 조회합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @GetMapping("/admin")
  public List<MemberSignupResponse> all(@AuthenticationPrincipal Jwt j) {
    return service.findAllForAdmin(id(j));
  }

  /** 관리자가 지정한 회원의 상태 변경을 서비스에 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/admin/{memberId}/status")
  public MemberSignupResponse status(
      @AuthenticationPrincipal Jwt j,
      @PathVariable Long memberId,
      @RequestParam MemberStatus status) {
    return service.updateStatus(id(j), memberId, status);
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long id(Jwt j) {
    return Long.valueOf(j.getSubject());
  }
}
