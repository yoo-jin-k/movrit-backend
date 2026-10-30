package com.movrit.movrit.memberaddress.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.memberaddress.dto.MemberAddressDtos;
import com.movrit.movrit.memberaddress.service.MemberAddressService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/addresses")
public class MemberAddressController {
  private final MemberAddressService service;

  @GetMapping
  public List<MemberAddressDtos.Response> list(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(JwtMemberId.from(jwt));
  }

  @PostMapping
  public ResponseEntity<MemberAddressDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberAddressDtos.SaveRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(JwtMemberId.from(jwt), request));
  }

  @PutMapping("/{addressId}")
  public MemberAddressDtos.Response update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long addressId,
      @Valid @RequestBody MemberAddressDtos.SaveRequest request) {
    return service.update(JwtMemberId.from(jwt), addressId, request);
  }

  @PatchMapping("/{addressId}/default")
  public MemberAddressDtos.Response setDefault(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {
    return service.setDefault(JwtMemberId.from(jwt), addressId);
  }

  @DeleteMapping("/{addressId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {
    service.delete(JwtMemberId.from(jwt), addressId);
    return ResponseEntity.noContent().build();
  }
}
