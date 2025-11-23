package dev.harscode.itsectest.application.auth;

public record RefreshTokenCommand(
        String rawRefreshToken,
        String userAgent,
        String ipAddress
) {
}
