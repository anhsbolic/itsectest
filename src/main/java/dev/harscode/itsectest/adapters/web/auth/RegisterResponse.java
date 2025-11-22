package dev.harscode.itsectest.adapters.web.auth;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String username,
        String email,
        String role,
        String status
) {
}
