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

    /**
     * JWT subject에는 로그인한 Member의 id가 들어 있다.
     * 요청 body에서 memberId를 받지 않고 토큰에서 꺼내 사용해 본인 명의로만 신청할 수 있게 한다.
     */
    @PostMapping("/apply")
    public ResponseEntity<SellerDtos.Response> apply(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SellerDtos.ApplyRequest request
    ) {
        Long memberId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerProfileService.apply(memberId, request));
    }

    @GetMapping("/me")
    public SellerDtos.Response findMine(@AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());
        return sellerProfileService.findMine(memberId);
    }
}
