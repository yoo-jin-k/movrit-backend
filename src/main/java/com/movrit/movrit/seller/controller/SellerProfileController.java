package com.movrit.movrit.seller.controller;

import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.service.SellerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sellers")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    @PostMapping
    public ResponseEntity<SellerDtos.Response> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SellerDtos.CreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerProfileService.create(memberId(jwt), request));
    }

    @GetMapping("/me")
    public SellerDtos.Response findMine(@AuthenticationPrincipal Jwt jwt) {
        return sellerProfileService.findMine(memberId(jwt));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<SellerDtos.Response> findAll() {
        return sellerProfileService.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public SellerDtos.Response findById(@PathVariable Long id) {
        return sellerProfileService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public SellerDtos.Response update(@PathVariable Long id, @Valid @RequestBody SellerDtos.UpdateRequest request) {
        return sellerProfileService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sellerProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
