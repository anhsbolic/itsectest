package dev.harscode.itsectest.security.jwt;

import java.util.UUID;

public record AccessTokenPayload(
        UUID userId,
        UUID sessionId,
        String role,
        boolean mfa
) {
}
