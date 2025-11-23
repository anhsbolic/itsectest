package dev.harscode.itsectest.adapters.web.user.dto;

public record CreateUserCommand(
        String username,
        String email,
        String password,
        String fullName,
        String role
) {
}
