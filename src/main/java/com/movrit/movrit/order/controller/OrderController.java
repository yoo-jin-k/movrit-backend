package com.movrit.movrit.order.controller;

import com.movrit.movrit.order.dto.OrderDtos;
import com.movrit.movrit.order.service.OrderService;
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
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDtos.Response> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrderDtos.CreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(memberId(jwt), request));
    }

    @GetMapping
    public List<OrderDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
        return orderService.findMine(memberId(jwt));
    }

    @GetMapping("/{id}")
    public OrderDtos.Response findMineById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.findMineById(memberId(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelMine(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        orderService.cancelMine(memberId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public OrderDtos.Response update(@PathVariable Long id, @Valid @RequestBody OrderDtos.UpdateRequest request) {
        return orderService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
