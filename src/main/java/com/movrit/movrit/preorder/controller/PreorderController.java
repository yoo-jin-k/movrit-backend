package com.movrit.movrit.preorder.controller;

import com.movrit.movrit.preorder.dto.PreorderDtos;
import com.movrit.movrit.preorder.service.PreorderService;
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
@RequestMapping("/api/preorders")
public class PreorderController {
    private final PreorderService preorderService;

    @PostMapping
    public ResponseEntity<PreorderDtos.Response> create(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody PreorderDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(preorderService.create(memberId(jwt), request));
    }

    @GetMapping("/mine")
    public List<PreorderDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
        return preorderService.findMine(memberId(jwt));
    }

    @GetMapping("/open")
    public List<PreorderDtos.Response> findOpen() {
        return preorderService.findOpen();
    }
    @GetMapping("/browse") public List<PreorderDtos.Response> browse(@RequestParam(required=false)String keyword,@RequestParam(required=false)String category,@RequestParam(required=false)com.movrit.movrit.preorder.entity.PreorderStatus status){return preorderService.browse(keyword,category,status);}

    @GetMapping("/{id}")
    public PreorderDtos.Response findById(@PathVariable Long id) {
        return preorderService.findById(id);
    }

    @PutMapping("/{id}")
    public PreorderDtos.Response update(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable Long id,
                                        @Valid @RequestBody PreorderDtos.UpdateRequest request) {
        return preorderService.update(memberId(jwt), id, request);
    }

    @PatchMapping("/{id}/schedule")
    public PreorderDtos.Response schedule(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return preorderService.schedule(memberId(jwt), id);
    }

    @PatchMapping("/{id}/open")
    public PreorderDtos.Response open(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return preorderService.open(memberId(jwt), id);
    }

    @PatchMapping("/{id}/close")
    public PreorderDtos.Response close(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return preorderService.close(memberId(jwt), id);
    }

    @PatchMapping("/{id}/finalize")
    public PreorderDtos.Response finalizeResult(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return preorderService.finalizeResult(memberId(jwt), id);
    }

    @PatchMapping("/{id}/cancel")
    public PreorderDtos.Response cancel(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable Long id,
                                        @Valid @RequestBody PreorderDtos.CancelRequest request) {
        return preorderService.cancel(memberId(jwt), id, request.reason());
    }

    @PatchMapping("/{id}/production") public PreorderDtos.Response production(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return preorderService.startProduction(memberId(jwt),id);}
    @PatchMapping("/{id}/shipping") public PreorderDtos.Response shipping(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return preorderService.startShipping(memberId(jwt),id);}
    @PatchMapping("/{id}/complete") public PreorderDtos.Response complete(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return preorderService.complete(memberId(jwt),id);}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        preorderService.delete(memberId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
