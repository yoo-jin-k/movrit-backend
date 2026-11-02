package com.movrit.movrit.member.controller;

import com.movrit.movrit.member.dto.MemberPasswordCapabilityResponse;
import com.movrit.movrit.member.dto.MemberPasswordChangeRequest;
import com.movrit.movrit.member.dto.MemberSignupRequest;
import com.movrit.movrit.member.dto.MemberSignupResponse;
import com.movrit.movrit.member.dto.MemberUpdateRequest;
import com.movrit.movrit.member.dto.MemberWithdrawalDtos;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.member.service.MemberWithdrawalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 회원의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@Tag(name = "공통 · 회원")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
  private final MemberService service;
  private final MemberWithdrawalService withdrawalService;

  /** 일반 회원가입을 요청합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PostMapping("/signup")
  public ResponseEntity<MemberSignupResponse> signup(@Valid @RequestBody MemberSignupRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.signup(r));
  }

  /** 로그인 회원 정보를 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/me")
  public MemberSignupResponse me(@AuthenticationPrincipal Jwt j) {
    return service.findById(id(j));
  }

  /** 내 회원 정보를 고칩니다. 요청한 회원은 로그인 토큰으로 확인합니다. 필수값·길이 같은 입력 형식을 먼저 검사한 뒤 처리합니다. */
  @PutMapping("/me")
  public MemberSignupResponse update(
      @AuthenticationPrincipal Jwt j, @Valid @RequestBody MemberUpdateRequest r) {
    return service.update(id(j), r);
  }

  /** 로그인 회원의 현재 비밀번호를 확인한 뒤 비밀번호를 변경하고 기존 재발급 세션을 폐기합니다. */
  @PatchMapping("/me/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberPasswordChangeRequest request) {
    service.changePassword(id(jwt), request);
    return ResponseEntity.noContent().build();
  }

  /** 로그인 회원의 비밀번호 보유 여부와 변경 가능 여부를 조회합니다. */
  @GetMapping("/me/password-capability")
  public MemberPasswordCapabilityResponse passwordCapability(@AuthenticationPrincipal Jwt jwt) {
    return service.passwordCapability(id(jwt));
  }

  /** 지금 탈퇴할 수 있는지(진행 중인 캠페인 등 막는 이유)를 확인합니다. */
  @GetMapping("/me/withdrawal")
  public MemberWithdrawalDtos.Eligibility withdrawalCheck(@AuthenticationPrincipal Jwt j) {
    return withdrawalService.check(id(j));
  }

  /** 로그인 회원 본인 탈퇴입니다. 확인 문구 '탈퇴합니다'가 필요하며, 진행 중인 캠페인이 있으면 409입니다. */
  @PostMapping("/me/withdrawal")
  public ResponseEntity<Void> withdraw(
      @AuthenticationPrincipal Jwt j,
      @Valid @RequestBody MemberWithdrawalDtos.WithdrawRequest request) {
    withdrawalService.withdraw(id(j), request.confirmText());
    return ResponseEntity.noContent().build();
  }

  /** 관리 대상 목록을 조회합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
  @GetMapping("/admin")
  public List<MemberSignupResponse> all(@AuthenticationPrincipal Jwt j) {
    return service.findAllForAdmin(id(j));
  }

  /** 관리자가 지정한 회원의 상태 변경을 서비스에 요청합니다. 요청한 회원은 로그인 토큰으로 확인합니다. */
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
