package com.movrit.movrit.product.controller;

import com.movrit.movrit.product.dto.ProductDtos;
import com.movrit.movrit.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDtos.Response> create(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody ProductDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(memberId(jwt), request));
    }

    @GetMapping("/mine")
    public List<ProductDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
        return productService.findMine(memberId(jwt));
    }

    @GetMapping("/{id}")
    public ProductDtos.Response findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PutMapping("/{id}")
    public ProductDtos.Response update(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ProductDtos.UpdateRequest request) {
        return productService.update(memberId(jwt), id, request);
    }

    @PostMapping("/{id}/thumbnail")
    public ProductDtos.Response uploadThumbnail(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable Long id,
                                                @RequestParam("file") MultipartFile file) {
        return productService.uploadThumbnail(memberId(jwt), id, file);
    }

    @PostMapping("/{id}/variants")
    public ProductDtos.VariantResponse createVariant(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody ProductDtos.VariantCreateRequest request) {
        return productService.createVariant(memberId(jwt), id, request);
    }

    @GetMapping("/{id}/variants")
    public List<ProductDtos.VariantResponse> findVariants(@PathVariable Long id) {
        return productService.findVariants(id);
    }

    @PatchMapping("/variants/{variantId}/restock")
    public ProductDtos.VariantResponse restock(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable Long variantId,
                                               @Valid @RequestBody ProductDtos.StockRequest request) {
        return productService.restock(memberId(jwt), variantId, request.quantity());
    }

    @PatchMapping("/variants/{variantId}/safety-stock")
    public ProductDtos.VariantResponse updateSafetyStock(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable Long variantId,
                                                         @Valid @RequestBody ProductDtos.SafetyStockRequest request) {
        return productService.updateSafetyStock(memberId(jwt), variantId, request.quantity());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        productService.delete(memberId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
