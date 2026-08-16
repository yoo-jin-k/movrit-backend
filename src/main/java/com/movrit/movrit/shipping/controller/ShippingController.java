package com.movrit.movrit.shipping.controller;

import com.movrit.movrit.shipping.dto.ShippingDtos;
import com.movrit.movrit.shipping.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipments")
public class ShippingController {
    private final ShippingService shippingService;

    @PostMapping
    public ResponseEntity<ShippingDtos.Response> create(@Valid @RequestBody ShippingDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shippingService.create(request));
    }

    @GetMapping
    public List<ShippingDtos.Response> findAll() {
        return shippingService.findAll();
    }

    @GetMapping("/{id}")
    public ShippingDtos.Response findById(@PathVariable Long id) {
        return shippingService.findById(id);
    }

    @PutMapping("/{id}")
    public ShippingDtos.Response update(@PathVariable Long id, @Valid @RequestBody ShippingDtos.UpdateRequest request) {
        return shippingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shippingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
