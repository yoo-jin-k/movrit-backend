package com.movrit.movrit.payment.controller;

import com.movrit.movrit.payment.dto.PaymentDtos;
import com.movrit.movrit.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDtos.Response> create(@Valid @RequestBody PaymentDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
    }

    @GetMapping
    public List<PaymentDtos.Response> findAll() {
        return paymentService.findAll();
    }

    @GetMapping("/{id}")
    public PaymentDtos.Response findById(@PathVariable Long id) {
        return paymentService.findById(id);
    }

    @PutMapping("/{id}")
    public PaymentDtos.Response update(@PathVariable Long id, @Valid @RequestBody PaymentDtos.UpdateRequest request) {
        return paymentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
