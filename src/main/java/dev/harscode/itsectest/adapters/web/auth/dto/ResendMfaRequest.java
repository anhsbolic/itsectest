package dev.harscode.itsectest.adapters.web.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendMfaRequest(
        @NotBlank String mfaSessionId
) {
}
