package dev.harscode.itsectest.application.auth.register;

import java.util.UUID;

public record RegisterUserResult(
        UUID userId,
        String username,
        String email,
        String role,
        String status
) {
}