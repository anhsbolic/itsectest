package dev.harscode.itsectest.application.auth.login;

public record LoginUserCommand(
        String usernameOrEmail,
        String rawPassword,
        String userAgent,
        String ipAddress
) {
}