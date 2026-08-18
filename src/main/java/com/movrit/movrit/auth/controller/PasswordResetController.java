package com.movrit.movrit.auth.controller;

import com.movrit.movrit.auth.dto.PasswordResetDtos;
import com.movrit.movrit.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot")
    public ResponseEntity<PasswordResetDtos.ForgotResponse> forgot(
            @Valid @RequestBody PasswordResetDtos.ForgotRequest request
    ) {
        return ResponseEntity.ok(passwordResetService.forgot(request.email()));
    }

    @GetMapping("/reset/validate")
    public PasswordResetDtos.ValidateResponse validate(@RequestParam String token) {
        return passwordResetService.validate(token);
    }

    @PostMapping("/reset")
    public PasswordResetDtos.MessageResponse reset(
            @Valid @RequestBody PasswordResetDtos.ResetRequest request
    ) {
        return passwordResetService.reset(request.token(), request.newPassword());
    }
}
