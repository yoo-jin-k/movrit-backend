package com.movrit.movrit.inquiry.controller;

import com.movrit.movrit.inquiry.dto.InquiryDtos;
import com.movrit.movrit.inquiry.entity.InquiryStatus;
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

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<InquiryDtos.Response> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InquiryDtos.CreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.create(memberId(jwt), request));
    }

    @GetMapping("/mine")
    public List<InquiryDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
        return inquiryService.findMine(memberId(jwt));
    }

    @GetMapping("/mine/{id}")
    public InquiryDtos.Response findMineById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return inquiryService.findMineById(memberId(jwt), id);
    }

    @PatchMapping("/{id}/close")
    public InquiryDtos.Response close(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return inquiryService.close(memberId(jwt), id);
    }

    @GetMapping("/admin")
    public List<InquiryDtos.Response> findForAdmin(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) InquiryStatus status
    ) {
        return inquiryService.findForAdmin(memberId(jwt), status);
    }

    @PatchMapping("/admin/{id}/answer")
    public InquiryDtos.Response answer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody InquiryDtos.AnswerRequest request
    ) {
        return inquiryService.answer(memberId(jwt), id, request);
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
