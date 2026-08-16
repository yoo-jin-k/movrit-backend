package com.movrit.movrit.settlement.controller;

import com.movrit.movrit.settlement.dto.SettlementDtos;
import com.movrit.movrit.settlement.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements")
public class SettlementController {
    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<SettlementDtos.Response> create(@Valid @RequestBody SettlementDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(settlementService.create(request));
    }

    @GetMapping
    public List<SettlementDtos.Response> findAll() {
        return settlementService.findAll();
    }

    @GetMapping("/{id}")
    public SettlementDtos.Response findById(@PathVariable Long id) {
        return settlementService.findById(id);
    }

    @PutMapping("/{id}")
    public SettlementDtos.Response update(@PathVariable Long id, @Valid @RequestBody SettlementDtos.UpdateRequest request) {
        return settlementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        settlementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
