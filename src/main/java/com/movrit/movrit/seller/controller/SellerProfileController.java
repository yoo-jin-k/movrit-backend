package com.movrit.movrit.seller.controller;

import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.service.SellerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sellers")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    @PostMapping
    public ResponseEntity<SellerDtos.Response> create(@Valid @RequestBody SellerDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerProfileService.create(request));
    }

    @GetMapping
    public List<SellerDtos.Response> findAll() {
        return sellerProfileService.findAll();
    }

    @GetMapping("/{id}")
    public SellerDtos.Response findById(@PathVariable Long id) {
        return sellerProfileService.findById(id);
    }

    @PutMapping("/{id}")
    public SellerDtos.Response update(@PathVariable Long id, @Valid @RequestBody SellerDtos.UpdateRequest request) {
        return sellerProfileService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sellerProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
