package dev.harscode.itsectest.application.auth;

public record LoginUserCommand(
        String usernameOrEmail,
        String rawPassword,
        String userAgent,
        String ipAddress
) {
}