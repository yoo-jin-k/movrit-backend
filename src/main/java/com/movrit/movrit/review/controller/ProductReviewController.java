package com.movrit.movrit.review.controller;

import com.movrit.movrit.review.dto.ProductReviewDtos;
import com.movrit.movrit.review.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ProductReviewController {

    private final ProductReviewService reviewService;

    @PostMapping
    public ResponseEntity<ProductReviewDtos.Response> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProductReviewDtos.CreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(id(jwt), request));
    }

    @GetMapping("/products/{productId}")
    public List<ProductReviewDtos.Response> productReviews(@PathVariable Long productId) {
        return reviewService.findByProduct(productId);
    }

    @GetMapping("/mine")
    public List<ProductReviewDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
        return reviewService.findMine(id(jwt));
    }

    @GetMapping("/seller")
    public List<ProductReviewDtos.Response> seller(@AuthenticationPrincipal Jwt jwt) {
        return reviewService.findSellerReviews(id(jwt));
    }

    @PutMapping("/{reviewId}")
    public ProductReviewDtos.Response update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId,
            @Valid @RequestBody ProductReviewDtos.UpdateRequest request
    ) {
        return reviewService.update(id(jwt), reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long reviewId) {
        reviewService.delete(id(jwt), reviewId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/reply")
    public ProductReviewDtos.Response reply(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId,
            @Valid @RequestBody ProductReviewDtos.ReplyRequest request
    ) {
        return reviewService.reply(id(jwt), reviewId, request);
    }

    private Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
