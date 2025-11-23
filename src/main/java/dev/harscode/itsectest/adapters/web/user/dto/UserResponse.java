package dev.harscode.itsectest.adapters.web.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String role,
        String status,
        boolean emailVerified,
        boolean mfaEnabled,
        Instant lastLoginAt,
        Instant createdAt
) {
}
