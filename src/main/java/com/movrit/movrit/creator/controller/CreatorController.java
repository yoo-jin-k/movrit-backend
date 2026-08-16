package com.movrit.movrit.creator.controller;

import com.movrit.movrit.creator.dto.CreatorDtos;
import com.movrit.movrit.creator.service.CreatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/creators")
public class CreatorController {
    private final CreatorService creatorService;

    @PostMapping
    public ResponseEntity<CreatorDtos.Response> create(@Valid @RequestBody CreatorDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creatorService.create(request));
    }

    @GetMapping
    public List<CreatorDtos.Response> findAll() {
        return creatorService.findAll();
    }

    @GetMapping("/{id}")
    public CreatorDtos.Response findById(@PathVariable Long id) {
        return creatorService.findById(id);
    }

    @PutMapping("/{id}")
    public CreatorDtos.Response update(@PathVariable Long id, @Valid @RequestBody CreatorDtos.UpdateRequest request) {
        return creatorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        creatorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
