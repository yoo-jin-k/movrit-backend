package com.movrit.movrit.inquiry.controller;

import com.movrit.movrit.inquiry.dto.InquiryDtos;
import com.movrit.movrit.inquiry.service.InquiryService;
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
@RequestMapping("/api/inquiries")
public class InquiryController {
    private final InquiryService service;

    @PostMapping
    public ResponseEntity<InquiryDtos.Response> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InquiryDtos.CreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(memberId(jwt), request));
    }

    @GetMapping("/mine")
    public List<InquiryDtos.Response> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.findMine(memberId(jwt));
    }

    @GetMapping("/admin")
    public List<InquiryDtos.Response> all(@AuthenticationPrincipal Jwt jwt) {
        return service.findAll(memberId(jwt));
    }

    @PatchMapping("/{inquiryId}/answer")
    public InquiryDtos.Response answer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryDtos.AnswerRequest request
    ) {
        return service.answer(memberId(jwt), inquiryId, request);
    }

    @PatchMapping("/{inquiryId}/close")
    public InquiryDtos.Response close(@AuthenticationPrincipal Jwt jwt, @PathVariable Long inquiryId) {
        return service.close(memberId(jwt), inquiryId);
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
