package com.movrit.movrit.preorder.controller;

import com.movrit.movrit.preorder.dto.PreorderDtos;
import com.movrit.movrit.preorder.service.PreorderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/preorders")
public class PreorderController {
    private final PreorderService preorderService;

    @PostMapping
    public ResponseEntity<PreorderDtos.Response> create(@Valid @RequestBody PreorderDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preorderService.create(request));
    }

    @GetMapping
    public List<PreorderDtos.Response> findAll() {
        return preorderService.findAll();
    }

    @GetMapping("/{id}")
    public PreorderDtos.Response findById(@PathVariable Long id) {
        return preorderService.findById(id);
    }

    @PutMapping("/{id}")
    public PreorderDtos.Response update(@PathVariable Long id, @Valid @RequestBody PreorderDtos.UpdateRequest request) {
        return preorderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        preorderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
