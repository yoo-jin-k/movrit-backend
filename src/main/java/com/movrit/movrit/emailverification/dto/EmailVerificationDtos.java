package com.movrit.movrit.emailverification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public final class EmailVerificationDtos {
  private EmailVerificationDtos() {}

  public record ConfirmRequest(
      @NotNull Long challengeId, @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code) {}

  public record RequestResponse(Long challengeId, String maskedEmail, LocalDateTime expiresAt) {}

  public record StatusResponse(
      boolean emailVerified,
      LocalDateTime emailVerifiedAt,
      boolean identityVerified,
      String applicationEligibilityPolicy) {}
}
