package com.movrit.movrit.brand.controller;

import com.movrit.movrit.brand.dto.BrandDtos;
import com.movrit.movrit.brand.entity.BrandStatus;
import com.movrit.movrit.brand.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public BrandDtos.Response create(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody BrandDtos.CreateRequest request) {
        return brandService.create(memberId(jwt), request);
    }

    @GetMapping("/mine")
    public List<BrandDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
        return brandService.findMine(memberId(jwt));
    }

    @PutMapping("/{brandId}")
    public BrandDtos.Response update(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable Long brandId,
                                     @Valid @RequestBody BrandDtos.UpdateRequest request) {
        return brandService.update(memberId(jwt), brandId, request);
    }

    @PostMapping("/{brandId}/logo")
    public BrandDtos.Response uploadLogo(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable Long brandId,
                                         @RequestParam("file") MultipartFile file) {
        return brandService.uploadLogo(memberId(jwt), brandId, file);
    }

    @PostMapping("/{brandId}/cover")
    public BrandDtos.Response uploadCover(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable Long brandId,
                                          @RequestParam("file") MultipartFile file) {
        return brandService.uploadCover(memberId(jwt), brandId, file);
    }

    @PatchMapping("/{brandId}/status")
    public BrandDtos.Response changeStatus(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable Long brandId,
                                           @RequestParam BrandStatus status) {
        return brandService.changeStatus(memberId(jwt), brandId, status);
    }

    @DeleteMapping("/{brandId}")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long brandId) {
        brandService.delete(memberId(jwt), brandId);
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
