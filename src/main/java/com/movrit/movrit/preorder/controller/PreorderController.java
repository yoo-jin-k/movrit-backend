package com.movrit.movrit.preorder.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.preorder.dto.PreorderDtos;
import com.movrit.movrit.preorder.service.PreorderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** 프리오더의 HTTP 요청을 서비스로 연결합니다. 인증 주체는 토큰에서 읽고, 업무 권한과 상태 검증은 호출한 서비스의 정책을 따릅니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/preorders")
public class PreorderController {
  private final PreorderService preorderService;

  /** 프리오더 관련 생성을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PostMapping
  public ResponseEntity<PreorderDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PreorderDtos.CreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(preorderService.create(memberId(jwt), request));
  }

  /** 프리오더 관련 로그인 회원에게 연결된 목록을 조회합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @GetMapping("/mine")
  public List<PreorderDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
    return preorderService.findMine(memberId(jwt));
  }

  /** 프리오더 관련 진행 중인 프리오더를 조회합니다. */
  @GetMapping("/open")
  public List<PreorderDtos.Response> findOpen() {
    return preorderService.findOpen();
  }

  /** 프리오더 관련 검색어·분류·상태로 프리오더를 검색합니다. */
  @GetMapping("/browse")
  public List<PreorderDtos.Response> browse(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) com.movrit.movrit.preorder.entity.PreorderStatus status) {
    return preorderService.browse(keyword, category, status);
  }

  /** 프리오더 관련 식별자로 상세 정보를 조회합니다. */
  @GetMapping("/{id}")
  public PreorderDtos.Response findById(@PathVariable Long id) {
    return preorderService.findById(id);
  }

  /** 프리오더 관련 수정을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PutMapping("/{id}")
  public PreorderDtos.Response update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @Valid @RequestBody PreorderDtos.UpdateRequest request) {
    return preorderService.update(memberId(jwt), id, request);
  }

  /** 프리오더 관련 프리오더 예약 상태 전환을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/{id}/schedule")
  public PreorderDtos.Response schedule(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return preorderService.schedule(memberId(jwt), id);
  }

  /** 프리오더 관련 모집 또는 진행 시작을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/{id}/open")
  public PreorderDtos.Response open(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return preorderService.open(memberId(jwt), id);
  }

  /** 프리오더 관련 종료 처리를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/{id}/close")
  public PreorderDtos.Response close(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return preorderService.close(memberId(jwt), id);
  }

  /** 프리오더 관련 프리오더 모집 결과 확정을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/{id}/finalize")
  public PreorderDtos.Response finalizeResult(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return preorderService.finalizeResult(memberId(jwt), id);
  }

  /** 프리오더 관련 취소 처리를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. 요청 본문의 선언된 입력 제약을 검증한 뒤 처리합니다. */
  @PatchMapping("/{id}/cancel")
  public PreorderDtos.Response cancel(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @Valid @RequestBody PreorderDtos.CancelRequest request) {
    return preorderService.cancel(memberId(jwt), id, request.reason());
  }

  /** 프리오더 관련 생산 시작 상태 전환을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/{id}/production")
  public PreorderDtos.Response production(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return preorderService.startProduction(memberId(jwt), id);
  }

  /** 프리오더 관련 배송 시작 상태 전환을 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/{id}/shipping")
  public PreorderDtos.Response shipping(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return preorderService.startShipping(memberId(jwt), id);
  }

  /** 프리오더 관련 프리오더 완료 처리를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @PatchMapping("/{id}/complete")
  public PreorderDtos.Response complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return preorderService.complete(memberId(jwt), id);
  }

  /** 프리오더 관련 삭제를 요청합니다. 요청자의 회원 ID는 인증 토큰에서 가져옵니다. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    preorderService.delete(memberId(jwt), id);
    return ResponseEntity.noContent().build();
  }

  /** 요청 본문의 회원 ID를 신뢰하지 않고 검증된 JWT subject를 업무 서비스의 회원 식별자로 변환합니다. */
  private Long memberId(Jwt jwt) {
    return JwtMemberId.from(jwt);
  }
}
