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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
  private final MemberService service;

  @PostMapping("/signup")
  public ResponseEntity<MemberSignupResponse> signup(@Valid @RequestBody MemberSignupRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.signup(r));
  }

  @GetMapping("/me")
  public MemberSignupResponse me(@AuthenticationPrincipal Jwt j) {
    return service.findById(id(j));
  }

  @PutMapping("/me")
  public MemberSignupResponse update(
      @AuthenticationPrincipal Jwt j, @Valid @RequestBody MemberUpdateRequest r) {
    return service.update(id(j), r);
  }

  @DeleteMapping("/me")
  public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Jwt j) {
    service.delete(id(j));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/admin")
  public List<MemberSignupResponse> all(@AuthenticationPrincipal Jwt j) {
    return service.findAllForAdmin(id(j));
  }

  @PatchMapping("/admin/{memberId}/status")
  public MemberSignupResponse status(
      @AuthenticationPrincipal Jwt j,
      @PathVariable Long memberId,
      @RequestParam MemberStatus status) {
    return service.updateStatus(id(j), memberId, status);
  }

  private Long id(Jwt j) {
    return Long.valueOf(j.getSubject());
  }
}
