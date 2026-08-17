package com.movrit.movrit.creator.controller;
import com.movrit.movrit.creator.dto.CreatorDtos; import com.movrit.movrit.creator.service.CreatorService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/creators")
public class CreatorController { private final CreatorService service;
 @PostMapping public ResponseEntity<CreatorDtos.Response> create(@AuthenticationPrincipal Jwt j,@Valid @RequestBody CreatorDtos.SaveRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(id(j),r));}
 @GetMapping("/me") public CreatorDtos.Response mine(@AuthenticationPrincipal Jwt j){return service.mine(id(j));}
 @GetMapping public List<CreatorDtos.Response> all(){return service.findAll();}
 @GetMapping("/{id}") public CreatorDtos.Response one(@PathVariable Long id){return service.findById(id);}
 @PutMapping("/me") public CreatorDtos.Response update(@AuthenticationPrincipal Jwt j,@Valid @RequestBody CreatorDtos.SaveRequest r){return service.update(id(j),r);}
 private Long id(Jwt j){return Long.valueOf(j.getSubject());}}
