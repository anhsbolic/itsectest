package dev.harscode.itsectest.application.auth.refreshtoken;

public record RefreshTokenCommand(
        String rawRefreshToken,
        String userAgent,
        String ipAddress
) {
}
