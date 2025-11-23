package dev.harscode.itsectest.application.auth.register;

public record RegisterUserCommand(
        String username,
        String email,
        String password,
        String name
) {
}