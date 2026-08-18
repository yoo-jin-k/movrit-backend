package com.movrit.movrit.favorite.controller;

import com.movrit.movrit.favorite.dto.FavoriteDtos;
import com.movrit.movrit.favorite.service.PreorderFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites/preorders")
public class PreorderFavoriteController {

    private final PreorderFavoriteService favoriteService;

    @PostMapping("/{preorderId}")
    public ResponseEntity<FavoriteDtos.Response> add(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable Long preorderId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoriteService.add(memberId(jwt), preorderId));
    }

    @DeleteMapping("/{preorderId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long preorderId) {
        favoriteService.remove(memberId(jwt), preorderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<FavoriteDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
        return favoriteService.findMine(memberId(jwt));
    }

    @GetMapping("/{preorderId}/status")
    public FavoriteDtos.StatusResponse status(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable Long preorderId) {
        return favoriteService.status(memberId(jwt), preorderId);
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
