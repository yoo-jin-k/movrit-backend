package com.movrit.movrit.memberaddress.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.memberaddress.dto.MemberAddressDtos;
import com.movrit.movrit.memberaddress.service.MemberAddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/** 로그인한 회원의 배송지 주소록 API입니다. 캠페인 신청 시 배송 정보를 불러오는 데 사용합니다. */
@Tag(name = "참여자 · 배송지")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/addresses")
public class MemberAddressController {
  private final MemberAddressService service;

  /** 내 배송지 목록을 기본 배송지부터 조회합니다. */
  @GetMapping
  public List<MemberAddressDtos.Response> list(@AuthenticationPrincipal Jwt jwt) {
    return service.findMine(JwtMemberId.from(jwt));
  }

  /** 배송지를 추가합니다. 성공하면 201을 돌려줍니다. */
  @PostMapping
  public ResponseEntity<MemberAddressDtos.Response> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberAddressDtos.SaveRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(JwtMemberId.from(jwt), request));
  }

  /** 내 배송지를 고칩니다. */
  @PutMapping("/{addressId}")
  public MemberAddressDtos.Response update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long addressId,
      @Valid @RequestBody MemberAddressDtos.SaveRequest request) {
    return service.update(JwtMemberId.from(jwt), addressId, request);
  }

  /** 지정한 배송지를 기본 배송지로 바꿉니다. */
  @PatchMapping("/{addressId}/default")
  public MemberAddressDtos.Response setDefault(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {
    return service.setDefault(JwtMemberId.from(jwt), addressId);
  }

  /** 내 배송지를 삭제합니다. 기본 배송지를 지우면 가장 최근 주소가 기본이 됩니다. */
  @DeleteMapping("/{addressId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {
    service.delete(JwtMemberId.from(jwt), addressId);
    return ResponseEntity.noContent().build();
  }
}
