package com.movrit.movrit.seller.controller;

import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.service.SellerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sellers")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    @PostMapping
    public ResponseEntity<SellerDtos.Response> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SellerDtos.ApplyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerProfileService.create(memberId(jwt), request));
    }

    @GetMapping("/me")
    public SellerDtos.Response findMine(@AuthenticationPrincipal Jwt jwt) {
        return sellerProfileService.findMine(memberId(jwt));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
