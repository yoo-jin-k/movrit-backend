package com.movrit.movrit.admin.controller;

import com.movrit.movrit.order.dto.OrderDtos;
import com.movrit.movrit.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping
    public List<OrderDtos.Response> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public OrderDtos.Response findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PutMapping("/{id}")
    public OrderDtos.Response update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody OrderDtos.UpdateRequest request) {
        return orderService.update(id, request);
    }
}
