package dev.harscode.itsectest.adapters.web.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyMfaRequest(
        @NotBlank String mfaSessionId,
        @NotBlank @Size(min = 6, max = 6) String otp
) {
}
