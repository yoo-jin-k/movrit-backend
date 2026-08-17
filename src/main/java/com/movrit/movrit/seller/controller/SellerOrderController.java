package com.movrit.movrit.seller.controller;

import com.movrit.movrit.order.dto.OrderDtos;
import com.movrit.movrit.order.service.OrderService;
import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seller/orders")
@PreAuthorize("hasAnyRole('BRAND','ADMIN')")
public class SellerOrderController {
    private final SellerProfileRepository sellerProfileRepository;
    private final OrderService orderService;

    @GetMapping
    public List<OrderDtos.Response> findMine(@AuthenticationPrincipal Jwt jwt) {
        SellerProfile seller = sellerProfileRepository.findByMemberId(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "판매자 프로필이 없어."));
        return orderService.findBySellerId(seller.getId());
    }
}
