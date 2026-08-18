package com.movrit.movrit.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasswordResetDtos {
    private PasswordResetDtos() {}

    public record ForgotRequest(
            @NotBlank @Email String email
    ) {}

    public record ForgotResponse(
            String message,
            String devResetToken
    ) {}

    public record ValidateResponse(
            boolean valid
    ) {}

    public record ResetRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}

    public record MessageResponse(String message) {}
}
