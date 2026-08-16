package com.movrit.movrit.order.controller;

import com.movrit.movrit.order.dto.OrderDtos;
import com.movrit.movrit.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDtos.Response> create(@Valid @RequestBody OrderDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping
    public List<OrderDtos.Response> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public OrderDtos.Response findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PutMapping("/{id}")
    public OrderDtos.Response update(@PathVariable Long id, @Valid @RequestBody OrderDtos.UpdateRequest request) {
        return orderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
